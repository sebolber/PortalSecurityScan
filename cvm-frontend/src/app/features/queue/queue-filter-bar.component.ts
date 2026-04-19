import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
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
  imports: [CommonModule, FormsModule, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card p-4" aria-label="Filter">
      <div class="filter-bar">
        <label class="form-group grow min-w-[14rem]">
          <span class="form-label">Produktversion (UUID)</span>
          <input
            class="input-field"
            type="text"
            [ngModel]="store.filter().productVersionId ?? ''"
            (ngModelChange)="auf('productVersionId', $event)"
            placeholder="z.B. 11111111-1111-..."
          />
        </label>

        <label class="form-group grow min-w-[14rem]">
          <span class="form-label">Umgebung (UUID)</span>
          <input
            class="input-field"
            type="text"
            [ngModel]="store.filter().environmentId ?? ''"
            (ngModelChange)="auf('environmentId', $event)"
            placeholder="z.B. 22222222-2222-..."
          />
        </label>

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
  `
})
export class QueueFilterBarComponent {
  readonly store = inject(QueueStore);
  readonly severities = SEVERITY_REIHENFOLGE;
  readonly statusChips = STATUS_CHIPS;

  auf(
    key: 'productVersionId' | 'environmentId',
    value: string
  ): void {
    const trimmed = value.trim();
    this.store.setFilter({ [key]: trimmed.length === 0 ? undefined : trimmed });
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
