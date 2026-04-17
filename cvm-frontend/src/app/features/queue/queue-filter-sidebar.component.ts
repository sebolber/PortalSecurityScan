import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QueueStore } from './queue-store';
import { SEVERITY_REIHENFOLGE } from './queue.types';
import { Severity } from '../../shared/components/severity-badge.component';

/**
 * Filter-Sidebar fuer die Queue. Arbeitet direkt auf dem
 * {@link QueueStore}; Aenderungen triggern oberhalb der Seite einen
 * {@code reload()}.
 */
@Component({
  selector: 'cvm-queue-filter-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside class="flex w-72 shrink-0 flex-col gap-4 border-r bg-light p-4">
      <h2 class="text-sm font-semibold uppercase text-zinc-500">Filter</h2>

      <label class="block text-sm">
        <span class="mb-1 block text-zinc-700">Produktversion (UUID)</span>
        <input
          class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
          type="text"
          [ngModel]="store.filter().productVersionId ?? ''"
          (ngModelChange)="auf('productVersionId', $event)"
          placeholder="z.B. 11111111-1111-..."
        />
      </label>

      <label class="block text-sm">
        <span class="mb-1 block text-zinc-700">Umgebung (UUID)</span>
        <input
          class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
          type="text"
          [ngModel]="store.filter().environmentId ?? ''"
          (ngModelChange)="auf('environmentId', $event)"
          placeholder="z.B. 22222222-2222-..."
        />
      </label>

      <label class="block text-sm">
        <span class="mb-1 block text-zinc-700">Status</span>
        <select
          class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
          [ngModel]="store.filter().status ?? ''"
          (ngModelChange)="status($event)"
        >
          <option value="">Offen (PROPOSED + NEEDS_REVIEW)</option>
          <option value="PROPOSED">PROPOSED</option>
          <option value="NEEDS_REVIEW">NEEDS_REVIEW</option>
        </select>
      </label>

      <label class="block text-sm">
        <span class="mb-1 block text-zinc-700">Vorschlagsquelle</span>
        <select
          class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
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

      <div class="text-sm">
        <span class="mb-1 block text-zinc-700">Severity</span>
        <div class="flex flex-wrap gap-1">
          @for (s of severities; track s) {
            <button
              type="button"
              class="rounded border px-2 py-0.5 text-xs"
              [class.border-primary]="aktiv(s)"
              [class.bg-primary]="aktiv(s)"
              [class.text-white]="aktiv(s)"
              [class.border-zinc-300]="!aktiv(s)"
              (click)="toggle(s)"
            >
              {{ s }}
            </button>
          }
        </div>
      </div>

      <button
        type="button"
        class="mt-2 rounded border border-zinc-300 px-3 py-1 text-sm hover:bg-zinc-50"
        (click)="reset()"
      >
        Filter zuruecksetzen
      </button>
    </aside>
  `
})
export class QueueFilterSidebarComponent {
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
