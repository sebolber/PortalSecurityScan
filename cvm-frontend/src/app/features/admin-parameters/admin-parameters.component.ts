import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import {
  SystemParameterAuditLogView,
  SystemParameterCreateRequest,
  SystemParameterService,
  SystemParameterType,
  SystemParameterUpdateRequest,
  SystemParameterView
} from '../../core/parameters/system-parameter.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

const PARAM_TYPES: readonly SystemParameterType[] = [
  'STRING',
  'INTEGER',
  'DECIMAL',
  'BOOLEAN',
  'EMAIL',
  'URL',
  'JSON',
  'PASSWORD',
  'SELECT',
  'MULTISELECT',
  'DATE',
  'TIMESTAMP',
  'TEXTAREA',
  'HOST',
  'IP'
];

interface FormState {
  paramKey: string;
  label: string;
  description: string;
  handbook: string;
  category: string;
  subcategory: string;
  type: SystemParameterType;
  value: string;
  defaultValue: string;
  required: boolean;
  validationRules: string;
  options: string;
  unit: string;
  sensitive: boolean;
  hotReload: boolean;
  adminOnly: boolean;
}

function leeresFormular(): FormState {
  return {
    paramKey: '',
    label: '',
    description: '',
    handbook: '',
    category: '',
    subcategory: '',
    type: 'STRING',
    value: '',
    defaultValue: '',
    required: false,
    validationRules: '',
    options: '',
    unit: '',
    sensitive: false,
    hotReload: false,
    adminOnly: true
  };
}

/**
 * Admin-Seite fuer System-Parameter (Vorlage aus PortalCore).
 *
 * <p>Listet alle Parameter des aktuellen Mandanten gruppiert nach
 * Kategorie und bietet Formulare zum Anlegen/Bearbeiten sowie zum
 * Aendern und Zuruecksetzen einzelner Werte. Sensitive Werte werden
 * vom Backend maskiert ausgeliefert.
 */
@Component({
  selector: 'cvm-admin-parameters',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CvmIconComponent,
    EmptyStateComponent
  ],
  templateUrl: './admin-parameters.component.html',
  styleUrls: ['./admin-parameters.component.scss']
})
export class AdminParametersComponent implements OnInit {
  private readonly service = inject(SystemParameterService);
  private readonly toast = inject(CvmToastService);

  readonly typen = PARAM_TYPES;

  readonly parameter = signal<readonly SystemParameterView[]>([]);
  readonly auditLog = signal<readonly SystemParameterAuditLogView[]>([]);
  readonly laedt = signal(true);
  readonly fehler = signal<string | null>(null);
  readonly pending = signal(false);
  readonly bearbeiteId = signal<string | null>(null);
  readonly formular = signal<FormState>(leeresFormular());
  readonly kategorieFilter = signal<string>('');
  readonly aktiverTab = signal<'parameter' | 'audit'>('parameter');

  readonly kategorien = computed<readonly string[]>(() => {
    const set = new Set<string>();
    for (const p of this.parameter()) {
      set.add(p.category);
    }
    return Array.from(set).sort();
  });

  readonly gefiltertesParameter = computed<readonly SystemParameterView[]>(
    () => {
      const cat = this.kategorieFilter();
      if (!cat) {
        return this.parameter();
      }
      return this.parameter().filter((p) => p.category === cat);
    }
  );

  async ngOnInit(): Promise<void> {
    await this.ladeParameter();
  }

  async ladeParameter(): Promise<void> {
    this.laedt.set(true);
    this.fehler.set(null);
    try {
      const list = await this.service.list();
      this.parameter.set(list);
    } catch (err) {
      this.fehler.set(
        err instanceof Error
          ? err.message
          : 'Parameter konnten nicht geladen werden.'
      );
    } finally {
      this.laedt.set(false);
    }
  }

  async ladeAuditLog(): Promise<void> {
    try {
      const list = await this.service.auditLog();
      this.auditLog.set(list);
    } catch (err) {
      this.fehler.set(
        err instanceof Error
          ? err.message
          : 'Audit-Log konnte nicht geladen werden.'
      );
    }
  }

  zeigeTab(tab: 'parameter' | 'audit'): void {
    this.aktiverTab.set(tab);
    if (tab === 'audit') {
      void this.ladeAuditLog();
    }
  }

  updateFeld<K extends keyof FormState>(key: K, value: FormState[K]): void {
    this.formular.update((f) => ({ ...f, [key]: value }));
  }

  neuerEintrag(): void {
    this.bearbeiteId.set(null);
    this.formular.set(leeresFormular());
  }

  zumBearbeiten(eintrag: SystemParameterView): void {
    this.bearbeiteId.set(eintrag.id);
    this.formular.set({
      paramKey: eintrag.paramKey,
      label: eintrag.label,
      description: eintrag.description ?? '',
      handbook: eintrag.handbook ?? '',
      category: eintrag.category,
      subcategory: eintrag.subcategory ?? '',
      type: eintrag.type,
      value: eintrag.sensitive ? '' : eintrag.value ?? '',
      defaultValue: eintrag.defaultValue ?? '',
      required: eintrag.required,
      validationRules: eintrag.validationRules ?? '',
      options: eintrag.options ?? '',
      unit: eintrag.unit ?? '',
      sensitive: eintrag.sensitive,
      hotReload: eintrag.hotReload,
      adminOnly: eintrag.adminOnly
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
        const update: SystemParameterUpdateRequest = {
          label: aktuell.label,
          description: aktuell.description || null,
          handbook: aktuell.handbook || null,
          category: aktuell.category,
          subcategory: aktuell.subcategory || null,
          type: aktuell.type,
          defaultValue: aktuell.defaultValue || null,
          required: aktuell.required,
          validationRules: aktuell.validationRules || null,
          options: aktuell.options || null,
          unit: aktuell.unit || null,
          sensitive: aktuell.sensitive,
          hotReload: aktuell.hotReload,
          adminOnly: aktuell.adminOnly
        };
        const saved = await this.service.update(id, update);
        this.toast.success('Parameter "' + saved.paramKey + '" gespeichert.', 4000);
      } else {
        const create: SystemParameterCreateRequest = {
          paramKey: aktuell.paramKey,
          label: aktuell.label,
          description: aktuell.description || null,
          handbook: aktuell.handbook || null,
          category: aktuell.category,
          subcategory: aktuell.subcategory || null,
          type: aktuell.type,
          value: aktuell.value || null,
          defaultValue: aktuell.defaultValue || null,
          required: aktuell.required,
          validationRules: aktuell.validationRules || null,
          options: aktuell.options || null,
          unit: aktuell.unit || null,
          sensitive: aktuell.sensitive,
          hotReload: aktuell.hotReload,
          adminOnly: aktuell.adminOnly
        };
        const saved = await this.service.create(create);
        this.toast.success('Parameter "' + saved.paramKey + '" angelegt.', 4000);
      }
      this.neuerEintrag();
      await this.ladeParameter();
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

  async wertAendern(eintrag: SystemParameterView): Promise<void> {
    const neu = window.prompt(
      'Neuer Wert fuer "' + eintrag.paramKey + '":',
      eintrag.sensitive ? '' : eintrag.value ?? ''
    );
    if (neu === null) {
      return;
    }
    const grund = window.prompt('Aenderungsgrund (optional):', '') ?? '';
    try {
      await this.service.changeValue(eintrag.id, { value: neu, reason: grund || null });
      this.toast.success('Wert von "' + eintrag.paramKey + '" geaendert.', 4000);
      await this.ladeParameter();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Wert-Aenderung fehlgeschlagen.'
      );
    }
  }

  async wertZuruecksetzen(eintrag: SystemParameterView): Promise<void> {
    const bestaetigt = window.confirm(
      'Parameter "' + eintrag.paramKey + '" auf Standardwert zuruecksetzen?'
    );
    if (!bestaetigt) {
      return;
    }
    try {
      await this.service.reset(eintrag.id);
      this.toast.success('"' + eintrag.paramKey + '" zurueckgesetzt.', 4000);
      await this.ladeParameter();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Reset fehlgeschlagen.'
      );
    }
  }

  async loesche(eintrag: SystemParameterView): Promise<void> {
    const bestaetigt = window.confirm(
      'Parameter "' + eintrag.paramKey + '" wirklich loeschen?'
    );
    if (!bestaetigt) {
      return;
    }
    try {
      await this.service.delete(eintrag.id);
      this.toast.success('Parameter "' + eintrag.paramKey + '" geloescht.', 4000);
      if (this.bearbeiteId() === eintrag.id) {
        this.neuerEintrag();
      }
      await this.ladeParameter();
    } catch (err) {
      this.fehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Loeschen fehlgeschlagen.'
      );
    }
  }
}
