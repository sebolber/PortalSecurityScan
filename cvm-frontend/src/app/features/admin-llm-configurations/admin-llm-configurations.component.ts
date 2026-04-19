import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import {
  LlmConfigurationCreateRequest,
  LlmConfigurationService,
  LlmConfigurationUpdateRequest,
  LlmConfigurationView,
  LlmProviderInfo
} from '../../core/llm-config/llm-configuration.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { UuidChipComponent } from '../../shared/components/uuid-chip.component';

interface FormState {
  name: string;
  description: string;
  provider: string;
  model: string;
  baseUrl: string;
  secret: string;
  secretClear: boolean;
  maxTokens: number | null;
  temperature: number | null;
  active: boolean;
}

function leeresFormular(): FormState {
  return {
    name: '',
    description: '',
    provider: 'openai',
    model: '',
    baseUrl: '',
    secret: '',
    secretClear: false,
    maxTokens: null,
    temperature: null,
    active: false
  };
}

/**
 * Admin-Seite fuer LLM-Konfigurationen (Iteration 34b, CVM-78).
 *
 * <p>Listet alle Konfigurationen des aktuellen Mandanten und bietet
 * Formulare zum Anlegen und Bearbeiten. Secrets werden beim Laden
 * nicht zurueckgespielt - das Formular zeigt nur
 * {@code secretHint} und erlaubt ein Neusetzen oder Loeschen.
 */
@Component({
  selector: 'cvm-admin-llm-configurations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
    EmptyStateComponent,
    UuidChipComponent
  ],
  templateUrl: './admin-llm-configurations.component.html',
  styleUrls: ['./admin-llm-configurations.component.scss']
})
export class AdminLlmConfigurationsComponent implements OnInit {
  private readonly service = inject(LlmConfigurationService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly spalten = [
    'name',
    'provider',
    'model',
    'baseUrl',
    'secret',
    'active',
    'uuid',
    'aktion'
  ];

  readonly konfigurationen = signal<readonly LlmConfigurationView[]>([]);
  readonly provider = signal<readonly LlmProviderInfo[]>([]);
  readonly laedt = signal(true);
  readonly fehler = signal<string | null>(null);
  readonly pending = signal(false);
  readonly bearbeiteId = signal<string | null>(null);
  readonly formular = signal<FormState>(leeresFormular());

  readonly ausgewaehlterProvider = computed(() => {
    const key = this.formular().provider;
    return this.provider().find((p) => p.provider === key) ?? null;
  });

  readonly aktiveKonfig = computed<LlmConfigurationView | null>(
    () => this.konfigurationen().find((k) => k.active) ?? null
  );

  async ngOnInit(): Promise<void> {
    await Promise.all([this.ladeProviderListe(), this.ladeKonfigurationen()]);
  }

  async ladeProviderListe(): Promise<void> {
    try {
      const list = await this.service.providers();
      this.provider.set(list);
      // Default-Provider nur setzen, wenn das Formular noch leer ist.
      if (!this.formular().provider && list.length > 0) {
        this.updateFeld('provider', list[0].provider);
      }
    } catch (err) {
      this.fehler.set(
        err instanceof Error ? err.message : 'Providerliste fehlgeschlagen.'
      );
    }
  }

  async ladeKonfigurationen(): Promise<void> {
    this.laedt.set(true);
    this.fehler.set(null);
    try {
      const list = await this.service.list();
      this.konfigurationen.set(list);
    } catch (err) {
      this.fehler.set(
        err instanceof Error
          ? err.message
          : 'Konfigurationen konnten nicht geladen werden.'
      );
    } finally {
      this.laedt.set(false);
    }
  }

  updateFeld<K extends keyof FormState>(key: K, value: FormState[K]): void {
    this.formular.update((f) => ({ ...f, [key]: value }));
  }

  neuerEintrag(): void {
    this.bearbeiteId.set(null);
    this.formular.set(leeresFormular());
  }

  zumBearbeiten(eintrag: LlmConfigurationView): void {
    this.bearbeiteId.set(eintrag.id);
    this.formular.set({
      name: eintrag.name,
      description: eintrag.description ?? '',
      provider: eintrag.provider,
      model: eintrag.model,
      baseUrl: eintrag.baseUrl ?? '',
      secret: '',
      secretClear: false,
      maxTokens: eintrag.maxTokens,
      temperature: eintrag.temperature,
      active: eintrag.active
    });
  }

  async speichere(form: NgForm): Promise<void> {
    if (form.invalid) {
      this.fehler.set('Bitte Pflichtfelder pruefen.');
      return;
    }
    this.pending.set(true);
    this.fehler.set(null);
    const aktuell = this.formular();
    try {
      const id = this.bearbeiteId();
      if (id) {
        const update: LlmConfigurationUpdateRequest = {
          name: aktuell.name,
          description: aktuell.description || null,
          provider: aktuell.provider,
          model: aktuell.model,
          baseUrl: aktuell.baseUrl ? aktuell.baseUrl : null,
          secret: aktuell.secret ? aktuell.secret : null,
          secretClear: aktuell.secretClear,
          maxTokens: aktuell.maxTokens,
          temperature: aktuell.temperature,
          active: aktuell.active
        };
        const saved = await this.service.update(id, update);
        this.snack.open(
          'Konfiguration "' + saved.name + '" gespeichert.',
          'OK',
          { duration: 4000 }
        );
      } else {
        const create: LlmConfigurationCreateRequest = {
          name: aktuell.name,
          description: aktuell.description || null,
          provider: aktuell.provider,
          model: aktuell.model,
          baseUrl: aktuell.baseUrl || null,
          secret: aktuell.secret || null,
          maxTokens: aktuell.maxTokens,
          temperature: aktuell.temperature,
          active: aktuell.active
        };
        const saved = await this.service.create(create);
        this.snack.open(
          'Konfiguration "' + saved.name + '" angelegt.',
          'OK',
          { duration: 4000 }
        );
      }
      this.neuerEintrag();
      await this.ladeKonfigurationen();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Speichern fehlgeschlagen.'
      );
    } finally {
      this.pending.set(false);
    }
  }

  async loesche(eintrag: LlmConfigurationView): Promise<void> {
    const bestaetigt = window.confirm(
      'LLM-Konfiguration "' + eintrag.name + '" wirklich loeschen?'
    );
    if (!bestaetigt) {
      return;
    }
    try {
      await this.service.delete(eintrag.id);
      this.snack.open(
        'Konfiguration "' + eintrag.name + '" geloescht.',
        'OK',
        { duration: 4000 }
      );
      if (this.bearbeiteId() === eintrag.id) {
        this.neuerEintrag();
      }
      await this.ladeKonfigurationen();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Loeschen fehlgeschlagen.'
      );
    }
  }

  async aktiviereDirekt(eintrag: LlmConfigurationView): Promise<void> {
    if (eintrag.active) {
      return;
    }
    try {
      await this.service.update(eintrag.id, { active: true });
      this.snack.open(
        '"' + eintrag.name + '" ist jetzt aktiv.',
        'OK',
        { duration: 4000 }
      );
      await this.ladeKonfigurationen();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Aktivierung fehlgeschlagen.'
      );
    }
  }
}
