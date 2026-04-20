import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { EnvironmentPickerComponent } from './environment-picker.component';
import { ProductVersionPickerComponent } from './product-version-picker.component';
import { QueueStore } from './queue-store';
import {
  AssessmentStatus,
  SEVERITY_REIHENFOLGE
} from './queue.types';
import { Severity } from '../../shared/components/severity-badge.component';

interface StatusChip {
  readonly key: AssessmentStatus | null;
  readonly label: string;
}

const STATUS_CHIPS: readonly StatusChip[] = [
  { key: null, label: 'ALLE' },
  { key: 'PROPOSED', label: 'PROPOSED' },
  { key: 'NEEDS_REVIEW', label: 'NEEDS_REVIEW' },
  { key: 'APPROVED', label: 'APPROVED' },
  { key: 'REJECTED', label: 'REJECTED' },
  { key: 'EXPIRED', label: 'EXPIRED' }
];

/**
 * Horizontaler Filter-Balken oberhalb der Queue-Tabelle.
 *
 * <p>Iteration 47: ersetzt die bisherige linke Sidebar, damit die Tabelle
 * die volle Breite der Seite nutzen kann. Filter-Verhalten ist identisch
 * mit der frueheren {@code QueueFilterSidebarComponent}.
 *
 * <p>Iteration 61 (CVM-62): Material-freie Umsetzung mit
 * `.filter-bar`, `.form-group`, `.input-field`, `.select-field`,
 * `.severity-chip`.
 *
 * <p>Iteration 82 (CVM-322): Status-Chips statt Select.
 */
@Component({
  selector: 'cvm-queue-filter-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CvmIconComponent,
    EnvironmentPickerComponent,
    ProductVersionPickerComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card p-4" aria-label="Filter">
      <div class="filter-bar">
        <div class="form-group grow min-w-[18rem]">
          <span class="form-label">Produktversion</span>
          <div class="flex items-stretch gap-1">
            <input
              class="input-field grow"
              type="text"
              [ngModel]="store.filter().productVersionId ?? ''"
              (ngModelChange)="auf('productVersionId', $event)"
              placeholder="UUID oder ueber Suche waehlen"
            />
            <button
              type="button"
              class="btn btn-secondary"
              data-testid="queue-product-version-picker-open"
              [attr.aria-label]="'Produkt-Version suchen'"
              title="Produkt-Version suchen"
              (click)="pvPickerOffen.set(true)"
            >
              <cvm-icon name="search" [size]="16"></cvm-icon>
            </button>
          </div>
          @if (pvLabel()) {
            <span class="text-caption" data-testid="queue-product-version-label">
              {{ pvLabel() }}
              <button
                type="button"
                class="btn-icon"
                (click)="pvLabel.set(null); auf('productVersionId', '')"
                [attr.aria-label]="'Produkt-Version-Filter entfernen'"
                title="Entfernen"
              >
                <cvm-icon name="close" [size]="14"></cvm-icon>
              </button>
            </span>
          }
        </div>

        <div class="form-group grow min-w-[18rem]">
          <span class="form-label">Umgebung</span>
          <div class="flex items-stretch gap-1">
            <input
              class="input-field grow"
              type="text"
              [ngModel]="store.filter().environmentId ?? ''"
              (ngModelChange)="auf('environmentId', $event)"
              placeholder="UUID oder ueber Suche waehlen"
            />
            <button
              type="button"
              class="btn btn-secondary"
              data-testid="queue-environment-picker-open"
              [attr.aria-label]="'Umgebung suchen'"
              title="Umgebung suchen"
              (click)="envPickerOffen.set(true)"
            >
              <cvm-icon name="search" [size]="16"></cvm-icon>
            </button>
          </div>
          @if (envLabel()) {
            <span class="text-caption" data-testid="queue-environment-label">
              {{ envLabel() }}
              <button
                type="button"
                class="btn-icon"
                (click)="envLabel.set(null); auf('environmentId', '')"
                [attr.aria-label]="'Umgebungs-Filter entfernen'"
                title="Entfernen"
              >
                <cvm-icon name="close" [size]="14"></cvm-icon>
              </button>
            </span>
          }
        </div>

        <div class="form-group">
          <span class="form-label">Status</span>
          <div
            class="inline-flex rounded-lg border border-border bg-surface overflow-hidden h-10"
            role="group"
            aria-label="Status-Filter"
          >
            @for (c of statusChips; track c.label) {
              <button
                type="button"
                class="px-3 text-xs font-semibold uppercase tracking-wide border-r border-border last:border-r-0"
                [class.bg-primary-muted]="(store.filter().status ?? null) === c.key"
                [class.text-primary]="(store.filter().status ?? null) === c.key"
                [attr.data-testid]="'queue-status-' + (c.key ?? 'ALL')"
                (click)="status(c.key)"
              >{{ c.label }}</button>
            }
          </div>
        </div>

        <label class="form-group">
          <span class="form-label">Vorschlagsquelle</span>
          <select
            class="select-field"
            [ngModel]="store.filter().source ?? ''"
            (ngModelChange)="source($event)"
          >
            <option value="">Alle</option>
            <option value="REUSE">REUSE</option>
            <option value="RULE">RULE</option>
            <option value="AI">AI</option>
            <option value="MANUAL">MANUAL</option>
          </select>
        </label>

        <div class="form-group">
          <span class="form-label">Severity</span>
          <div class="inline-flex h-10 items-center gap-1 flex-wrap">
            @for (s of severities; track s) {
              <button
                type="button"
                class="severity-chip cursor-pointer border border-border transition-opacity"
                [attr.data-sev]="s"
                [class.opacity-40]="!aktiv(s)"
                (click)="toggle(s)"
                [attr.aria-pressed]="aktiv(s)"
              >
                {{ s }}
              </button>
            }
          </div>
        </div>

        <div class="form-group">
          <span class="form-label">&nbsp;</span>
          <button
            type="button"
            class="btn btn-secondary"
            data-testid="queue-filter-reset"
            (click)="reset()"
          >
            <cvm-icon name="clear" [size]="16"></cvm-icon>
            Filter zuruecksetzen
          </button>
        </div>
      </div>
    </section>

    <cvm-product-version-picker
      [visible]="pvPickerOffen()"
      (close)="pvPickerOffen.set(false)"
      (selected)="uebernehmeProduktVersion($event)"
    ></cvm-product-version-picker>

    <cvm-environment-picker
      [visible]="envPickerOffen()"
      (close)="envPickerOffen.set(false)"
      (selected)="uebernehmeUmgebung($event)"
    ></cvm-environment-picker>
  `
})
export class QueueFilterBarComponent {
  readonly store = inject(QueueStore);
  readonly severities = SEVERITY_REIHENFOLGE;
  readonly statusChips = STATUS_CHIPS;

  // Iteration 98 (CVM-340): Suchdialoge fuer Produkt-Version und Umgebung.
  readonly pvPickerOffen = signal<boolean>(false);
  readonly envPickerOffen = signal<boolean>(false);
  readonly pvLabel = signal<string | null>(null);
  readonly envLabel = signal<string | null>(null);

  uebernehmeProduktVersion(ev: { versionId: string; label: string }): void {
    this.store.setFilter({ productVersionId: ev.versionId });
    this.pvLabel.set(ev.label);
    this.pvPickerOffen.set(false);
  }

  uebernehmeUmgebung(ev: { environmentId: string; label: string }): void {
    this.store.setFilter({ environmentId: ev.environmentId });
    this.envLabel.set(ev.label);
    this.envPickerOffen.set(false);
  }

  auf(
    key: 'productVersionId' | 'environmentId',
    value: string
  ): void {
    const trimmed = value.trim();
    this.store.setFilter({ [key]: trimmed.length === 0 ? undefined : trimmed });
    if (key === 'productVersionId') this.pvLabel.set(null);
    if (key === 'environmentId') this.envLabel.set(null);
  }

  status(key: AssessmentStatus | null): void {
    this.store.setFilter({ status: key ?? undefined });
  }

  source(value: string): void {
    this.store.setFilter({
      source: value === '' ? undefined : (value as 'REUSE' | 'RULE' | 'AI' | 'MANUAL')
    });
  }

  toggle(severity: Severity): void {
    this.store.toggleSeverityFilter(severity);
  }

  aktiv(severity: Severity): boolean {
    return this.store.filter().severityIn?.includes(severity) ?? false;
  }

  reset(): void {
    this.store.resetFilter();
  }
}
