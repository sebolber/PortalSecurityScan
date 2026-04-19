import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import {
  LlmConfigurationCreateRequest,
  LlmConfigurationService,
  LlmConfigurationTestRequest,
  LlmConfigurationTestResult,
  LlmConfigurationUpdateRequest,
  LlmConfigurationView,
  LlmProviderInfo
} from '../../core/llm-config/llm-configuration.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
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
    CvmIconComponent,
    EmptyStateComponent,
    UuidChipComponent
  ],
  templateUrl: './admin-llm-configurations.component.html',
  styleUrls: ['./admin-llm-configurations.component.scss']
})
export class AdminLlmConfigurationsComponent implements OnInit {
  private readonly service = inject(LlmConfigurationService);
  private readonly toast = inject(CvmToastService);

  readonly konfigurationen = signal<readonly LlmConfigurationView[]>([]);
  readonly provider = signal<readonly LlmProviderInfo[]>([]);
  readonly laedt = signal(true);
  readonly fehler = signal<string | null>(null);
  readonly pending = signal(false);
  readonly testend = signal<string | null>(null);
  readonly testErgebnis = signal<LlmConfigurationTestResult | null>(null);
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
    this.testErgebnis.set(null);
  }

  zumBearbeiten(eintrag: LlmConfigurationView): void {
    this.bearbeiteId.set(eintrag.id);
    this.testErgebnis.set(null);
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
        this.toast.success('Konfiguration "' + saved.name + '" gespeichert.', 4000);
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
        this.toast.success('Konfiguration "' + saved.name + '" angelegt.', 4000);
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
      this.toast.success('Konfiguration "' + eintrag.name + '" geloescht.', 4000);
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

  /**
   * Test der aktuellen Formularwerte. Wenn gerade eine gespeicherte
   * Konfiguration bearbeitet wird und der Admin kein neues Secret
   * eingetippt hat, faellt der Server auf das DB-Secret zurueck - das
   * Feld bleibt leer, damit der Klartext nie durch die UI geht.
   */
  async testeFormular(): Promise<void> {
    const aktuell = this.formular();
    if (!aktuell.provider || !aktuell.model) {
      this.fehler.set('Provider und Modell sind fuer den Test erforderlich.');
      return;
    }
    const id = this.bearbeiteId();
    const payload: LlmConfigurationTestRequest = {
      provider: aktuell.provider,
      model: aktuell.model,
      baseUrl: aktuell.baseUrl ? aktuell.baseUrl : null,
      secret: aktuell.secret ? aktuell.secret : null
    };
    this.testErgebnis.set(null);
    this.testend.set(id ?? '__formular__');
    this.fehler.set(null);
    try {
      const ergebnis = id
        ? await this.service.testSaved(id, payload)
        : await this.service.testAdhoc(payload);
      this.testErgebnis.set(ergebnis);
      if (ergebnis.success) {
        this.toast.success('Test erfolgreich: ' + ergebnis.message, 4000);
      } else {
        this.toast.error('Test fehlgeschlagen: ' + ergebnis.message);
      }
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Testlauf fehlgeschlagen.'
      );
    } finally {
      this.testend.set(null);
    }
  }

  /**
   * Test einer gespeicherten Konfiguration direkt aus der Tabelle -
   * ohne das Formular zu veraendern. Secret und andere Felder kommen
   * aus der DB.
   */
  async testeGespeichert(eintrag: LlmConfigurationView): Promise<void> {
    this.testend.set(eintrag.id);
    this.fehler.set(null);
    try {
      const ergebnis = await this.service.testSaved(eintrag.id);
      if (ergebnis.success) {
        this.toast.success('Test erfolgreich: ' + ergebnis.message, 4000);
      } else {
        this.toast.error('Test fehlgeschlagen: ' + ergebnis.message);
      }
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Testlauf fehlgeschlagen.'
      );
    } finally {
      this.testend.set(null);
    }
  }

  async aktiviereDirekt(eintrag: LlmConfigurationView): Promise<void> {
    if (eintrag.active) {
      return;
    }
    try {
      await this.service.update(eintrag.id, { active: true });
      this.toast.success('"' + eintrag.name + '" ist jetzt aktiv.', 4000);
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
