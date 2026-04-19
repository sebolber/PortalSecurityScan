import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { QueueStore } from './queue-store';
import { SEVERITY_REIHENFOLGE } from './queue.types';
import { Severity } from '../../shared/components/severity-badge.component';

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

        <label class="form-group">
          <span class="form-label">Status</span>
          <select
            class="select-field"
            [ngModel]="store.filter().status ?? ''"
            (ngModelChange)="status($event)"
          >
            <option value="">Offen (PROPOSED + NEEDS_REVIEW)</option>
            <option value="PROPOSED">PROPOSED</option>
            <option value="NEEDS_REVIEW">NEEDS_REVIEW</option>
          </select>
        </label>

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

  auf(
    key: 'productVersionId' | 'environmentId',
    value: string
  ): void {
    const trimmed = value.trim();
    this.store.setFilter({ [key]: trimmed.length === 0 ? undefined : trimmed });
  }

  status(value: string): void {
    this.store.setFilter({
      status: value === '' ? undefined : (value as 'PROPOSED' | 'NEEDS_REVIEW')
    });
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
