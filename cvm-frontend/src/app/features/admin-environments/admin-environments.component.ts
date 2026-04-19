import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { CvmConfirmService } from '../../shared/components/cvm-confirm.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { UuidChipComponent } from '../../shared/components/uuid-chip.component';

const STAGES = ['DEV', 'TEST', 'REF', 'ABN', 'PROD'] as const;

interface EnvFormState {
  key: string;
  name: string;
  stage: (typeof STAGES)[number];
  tenant: string;
}

function initialForm(): EnvFormState {
  return { key: '', name: '', stage: 'DEV', tenant: 'default' };
}

/**
 * Admin-Verwaltung fuer Umgebungen (Iteration 28e, CVM-69).
 * Tabelle aller Umgebungen + Formular zum Anlegen einer neuen
 * (REF-/ABN-/PROD-/CI-Stage). Aenderung/Deaktivierung folgt in
 * einer spaeteren Iteration, sobald der Backend-CRUD dies abdeckt.
 */
@Component({
  selector: 'cvm-admin-environments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AhsBannerComponent,
    CvmIconComponent,
    EmptyStateComponent,
    UuidChipComponent
  ],
  templateUrl: './admin-environments.component.html',
  styleUrls: ['./admin-environments.component.scss']
})
export class AdminEnvironmentsComponent implements OnInit {
  private readonly service = inject(EnvironmentsService);
  private readonly toast = inject(CvmToastService);
  private readonly confirmService = inject(CvmConfirmService);

  readonly stages = STAGES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly rows = signal<readonly EnvironmentView[]>([]);
  readonly form = signal<EnvFormState>(initialForm());
  readonly formOpen = signal(false);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.service.list());
    } catch {
      this.error.set('Umgebungen konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  formularUmschalten(): void {
    this.formOpen.update((open) => !open);
    this.saveError.set(null);
  }

  update<K extends keyof EnvFormState>(key: K, value: EnvFormState[K]): void {
    this.form.update((f) => ({ ...f, [key]: value }));
  }

  async anlegen(): Promise<void> {
    const f = this.form();
    if (!f.key.trim() || !f.name.trim()) {
      this.saveError.set('key und name sind Pflichtfelder.');
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = await this.service.create({
        key: f.key.trim(),
        name: f.name.trim(),
        stage: f.stage,
        tenant: f.tenant.trim() || null
      });
      this.toast.success('Umgebung "' + saved.key + '" angelegt.', 4000);
      this.form.set(initialForm());
      this.formOpen.set(false);
      await this.laden();
    } catch (err) {
      this.saveError.set(
        err instanceof Error && err.message
          ? err.message
          : 'Anlage fehlgeschlagen (doppelter key?).'
      );
    } finally {
      this.saving.set(false);
    }
  }

  trackId(_: number, env: EnvironmentView): string {
    return env.id;
  }

  async loesche(env: EnvironmentView): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Umgebung entfernen',
      message: 'Umgebung "' + env.key + '" wirklich entfernen?\n\n'
        + 'Soft-Delete: Scans und Findings bleiben erhalten, die Umgebung '
        + 'verschwindet nur aus den Listen.',
      confirmLabel: 'Entfernen',
      variant: 'danger'
    });
    if (!confirmed) {
      return;
    }
    try {
      await this.service.delete(env.id);
      this.toast.success('Umgebung "' + env.key + '" entfernt.', 4000);
      await this.laden();
    } catch {
      this.toast.error('Loeschen fehlgeschlagen.');
    }
  }
}
