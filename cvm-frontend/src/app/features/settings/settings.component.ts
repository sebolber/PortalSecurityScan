import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import { Locale, LocaleService } from '../../core/i18n/locale.service';
import {
  LlmProvider,
  ModelProfileCreateRequest,
  ModelProfileService,
  ModelProfileView
} from '../../core/modelprofile/model-profile.service';
import { ThemeService } from '../../core/theme/theme.service';

const PRODUCT_STORAGE_KEY = 'cvm.default-product';
const PRODUCTS = [
  { key: 'PortalCore-Test', label: 'PortalCore-Test (1.14.2-test)' },
  { key: 'SmileKH-Test', label: 'SmileKH-Test' }
] as const;

@Component({
  selector: 'cvm-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly locale = inject(LocaleService);
  private readonly envService = inject(EnvironmentsService);
  private readonly profileService = inject(ModelProfileService);
  private readonly snackBar = inject(MatSnackBar);

  readonly username = this.auth.username;
  readonly themeMode = this.theme.mode;

  readonly locales: readonly { value: Locale; label: string }[] = [
    { value: 'de', label: 'Deutsch' },
    { value: 'en', label: 'English (in Vorbereitung)' }
  ];
  readonly aktuellesLocale = this.locale.locale;

  readonly products = PRODUCTS;
  defaultProduct = this.readStoredProduct();

  readonly isAdmin = computed(() =>
    this.auth.userRoles().includes(CVM_ROLES.ADMIN)
  );

  readonly environments = signal<readonly EnvironmentView[]>([]);
  readonly profiles = signal<readonly ModelProfileView[]>([]);
  readonly loadingAdmin = signal<boolean>(false);
  readonly adminError = signal<string | null>(null);

  /**
   * Formzustand pro Umgebung fuer den Profil-Wechsel.
   * key = environmentId.
   */
  readonly switchForms = signal<Record<string, {
    newProfileId: string;
    fourEyesConfirmer: string;
    reason: string;
    pending: boolean;
  }>>({});

  /** Formzustand der Profil-Anlage (einmalig pro Seite). */
  readonly createForm = signal<{
    profileKey: string;
    provider: LlmProvider;
    modelId: string;
    modelVersion: string;
    costBudgetEurMonthly: number;
    approvedForGkvData: boolean;
    fourEyesConfirmer: string;
    reason: string;
    pending: boolean;
    expanded: boolean;
  }>({
    profileKey: '',
    provider: 'CLAUDE_CLOUD',
    modelId: '',
    modelVersion: '',
    costBudgetEurMonthly: 0,
    approvedForGkvData: false,
    fourEyesConfirmer: '',
    reason: '',
    pending: false,
    expanded: false
  });

  ngOnInit(): void {
    if (this.isAdmin()) {
      void this.ladeAdminDaten();
    }
  }

  setTheme(dark: boolean): void {
    this.theme.set(dark ? 'dark' : 'light');
  }

  setLocale(locale: Locale): void {
    this.locale.setLocale(locale);
  }

  setDefaultProduct(key: string): void {
    this.defaultProduct = key;
    try {
      window.localStorage.setItem(PRODUCT_STORAGE_KEY, key);
    } catch {
      // Storage z.B. im Inkognito-Modus blockiert.
    }
  }

  async ladeAdminDaten(): Promise<void> {
    this.loadingAdmin.set(true);
    this.adminError.set(null);
    try {
      const [envs, profiles] = await Promise.all([
        this.envService.list(),
        this.profileService.list()
      ]);
      this.environments.set(envs);
      this.profiles.set(profiles);
      const initial: Record<string, {
        newProfileId: string;
        fourEyesConfirmer: string;
        reason: string;
        pending: boolean;
      }> = {};
      for (const env of envs) {
        initial[env.id] = {
          newProfileId: env.llmModelProfileId ?? '',
          fourEyesConfirmer: '',
          reason: '',
          pending: false
        };
      }
      this.switchForms.set(initial);
    } catch (err) {
      this.adminError.set(
        'Admin-Daten konnten nicht geladen werden.  ' +
          'Details siehe Netzwerk-Konsole.'
      );
    } finally {
      this.loadingAdmin.set(false);
    }
  }

  profileKey(id: string | null): string {
    if (!id) {
      return '(keines)';
    }
    return this.profiles().find((p) => p.id === id)?.profileKey ?? id;
  }

  async wechsleProfil(envId: string): Promise<void> {
    const forms = { ...this.switchForms() };
    const form = forms[envId];
    if (!form || !form.newProfileId || !form.fourEyesConfirmer.trim()) {
      this.snackBar.open(
        'Neues Profil und Vier-Augen-Freigeber sind Pflicht.',
        'OK',
        { duration: 3000 }
      );
      return;
    }
    const changedBy = this.username() || 'unknown';
    if (form.fourEyesConfirmer.trim() === changedBy) {
      this.snackBar.open(
        'Vier-Augen-Prinzip: Freigeber darf nicht gleich dem Anmelder sein.',
        'OK',
        { duration: 4000 }
      );
      return;
    }
    form.pending = true;
    this.switchForms.set({ ...forms, [envId]: form });
    try {
      await this.profileService.switch(envId, {
        newProfileId: form.newProfileId,
        changedBy,
        fourEyesConfirmer: form.fourEyesConfirmer.trim(),
        reason: form.reason.trim() || null
      });
      this.snackBar.open('Profil-Wechsel erfolgreich.', 'OK',
        { duration: 3000 });
      await this.ladeAdminDaten();
    } catch {
      // ApiClient zeigt Snackbar bereits.
    } finally {
      const nf = { ...this.switchForms() };
      if (nf[envId]) {
        nf[envId] = { ...nf[envId], pending: false };
        this.switchForms.set(nf);
      }
    }
  }

  toggleCreateForm(): void {
    const f = this.createForm();
    this.createForm.set({ ...f, expanded: !f.expanded });
  }

  updateCreateForm(patch: Partial<{
    profileKey: string;
    provider: LlmProvider;
    modelId: string;
    modelVersion: string;
    costBudgetEurMonthly: number;
    approvedForGkvData: boolean;
    fourEyesConfirmer: string;
    reason: string;
  }>): void {
    this.createForm.set({ ...this.createForm(), ...patch });
  }

  async legeProfilAn(): Promise<void> {
    const f = this.createForm();
    if (!f.profileKey.trim() || !f.modelId.trim()) {
      this.snackBar.open('profileKey und modelId sind Pflicht.', 'OK',
        { duration: 3000 });
      return;
    }
    if (!/^[A-Z0-9_]{2,64}$/.test(f.profileKey.trim())) {
      this.snackBar.open(
        'profileKey muss aus Grossbuchstaben, Ziffern und Unterstrichen bestehen (2-64 Zeichen).',
        'OK', { duration: 4000 });
      return;
    }
    if (f.costBudgetEurMonthly < 0) {
      this.snackBar.open('Budget muss >= 0 sein.', 'OK', { duration: 3000 });
      return;
    }
    const approvedBy = this.username() || 'unknown';
    if (f.approvedForGkvData) {
      if (!f.fourEyesConfirmer.trim()) {
        this.snackBar.open(
          'Bei GKV-Freigabe ist ein Vier-Augen-Freigeber Pflicht.',
          'OK', { duration: 4000 });
        return;
      }
      if (f.fourEyesConfirmer.trim() === approvedBy) {
        this.snackBar.open(
          'Vier-Augen-Prinzip: Freigeber darf nicht gleich dem Anlegenden sein.',
          'OK', { duration: 4000 });
        return;
      }
    }

    this.createForm.set({ ...f, pending: true });
    try {
      const req: ModelProfileCreateRequest = {
        profileKey: f.profileKey.trim(),
        provider: f.provider,
        modelId: f.modelId.trim(),
        modelVersion: f.modelVersion.trim() || null,
        costBudgetEurMonthly: f.costBudgetEurMonthly,
        approvedForGkvData: f.approvedForGkvData,
        approvedBy,
        fourEyesConfirmer: f.fourEyesConfirmer.trim() || null,
        reason: f.reason.trim() || null
      };
      const created = await this.profileService.createProfile(req);
      this.snackBar.open(
        `Modell-Profil "${created.profileKey}" angelegt.`, 'OK',
        { duration: 3000 });
      this.createForm.set({
        profileKey: '',
        provider: 'CLAUDE_CLOUD',
        modelId: '',
        modelVersion: '',
        costBudgetEurMonthly: 0,
        approvedForGkvData: false,
        fourEyesConfirmer: '',
        reason: '',
        pending: false,
        expanded: false
      });
      await this.ladeAdminDaten();
    } catch (err) {
      this.snackBar.open(this.formatProfileCreateError(err), 'OK',
        { duration: 5000 });
      this.createForm.set({ ...this.createForm(), pending: false });
    }
  }

  private formatProfileCreateError(err: unknown): string {
    if (err && typeof err === 'object') {
      const obj = err as { status?: number;
        error?: { error?: string; message?: string } };
      if (obj.status === 409 && obj.error?.error === 'profile_key_conflict') {
        return 'profileKey ist bereits vergeben.';
      }
      if (obj.status === 409 && obj.error?.error === 'vier_augen_violation') {
        return 'Vier-Augen-Verstoss: Freigeber darf nicht gleich dem Anleger sein.';
      }
      if (obj.status === 403) {
        return 'Keine Berechtigung. CVM_ADMIN erforderlich.';
      }
      if (obj.error?.message) {
        return obj.error.message;
      }
    }
    return 'Anlage fehlgeschlagen.';
  }

  updateForm(envId: string, patch: Partial<{
    newProfileId: string;
    fourEyesConfirmer: string;
    reason: string;
  }>): void {
    const forms = { ...this.switchForms() };
    const base = forms[envId] ?? {
      newProfileId: '',
      fourEyesConfirmer: '',
      reason: '',
      pending: false
    };
    forms[envId] = { ...base, ...patch };
    this.switchForms.set(forms);
  }

  private readStoredProduct(): string {
    try {
      return (
        window.localStorage.getItem(PRODUCT_STORAGE_KEY) ??
        PRODUCTS[0]?.key ??
        ''
      );
    } catch {
      return PRODUCTS[0]?.key ?? '';
    }
  }
}
