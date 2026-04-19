import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { QueueEntry } from './queue.types';

/**
 * Tabellenansicht der Queue-Eintraege. Die Komponente bleibt stateless
 * und erhaelt Auswahl/Pending von aussen.
 *
 * Iteration 61 (CVM-62): Verwendet `.data-table` und `.table-card`
 * aus `styles.scss`. Lokale Regeln nur fuer aktive/pending-Zustand.
 */
@Component({
  selector: 'cvm-queue-table',
  standalone: true,
  imports: [CommonModule, SeverityBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [
    `
      tr.ausgewaehlt {
        background-color: var(--color-primary-muted);
      }
      tr.pending {
        opacity: 0.5;
      }
    `
  ],
  template: `
    <div class="table-card">
      <table class="data-table data-table--compact">
        <thead>
          <tr>
            <th class="w-10">
              <span class="sr-only">Auswahl</span>
            </th>
            <th class="w-32">CVE</th>
            <th>Komponente</th>
            <th class="w-28">Original</th>
            <th class="w-28">Vorschlag</th>
            <th class="w-24">Quelle</th>
            <th class="w-28">Status</th>
            <th class="w-20">Alter</th>
          </tr>
        </thead>
        <tbody>
          @for (eintrag of entries; track eintrag.id) {
            <tr
              class="cursor-pointer"
              [class.ausgewaehlt]="eintrag.id === selectedId"
              [class.pending]="pendingIds.has(eintrag.id)"
              (click)="select.emit(eintrag.id)"
              [attr.aria-selected]="eintrag.id === selectedId"
            >
              <td>
                <input
                  type="checkbox"
                  [checked]="checkedIds.has(eintrag.id)"
                  (click)="$event.stopPropagation()"
                  (change)="toggle.emit(eintrag.id)"
                  [attr.aria-label]="'Auswahl ' + eintrag.cveKey"
                />
              </td>
              <td class="font-mono text-xs">{{ eintrag.cveKey }}</td>
              <td>{{ eintrag.rationale || '–' }}</td>
              <td>
                <ahs-severity-badge [severity]="eintrag.severity"></ahs-severity-badge>
              </td>
              <td>
                <ahs-severity-badge [severity]="eintrag.severity"></ahs-severity-badge>
              </td>
              <td class="text-xs uppercase tracking-wide">
                {{ eintrag.source }}
              </td>
              <td class="text-xs uppercase tracking-wide">
                {{ eintrag.status }}
              </td>
              <td class="text-xs text-text-muted">
                {{ alter(eintrag.createdAt) }}
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="8" class="px-3 py-8 text-center text-sm text-text-muted">
                Keine offenen Vorschlaege fuer die aktuellen Filter.
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class QueueTableComponent {
  @Input({ required: true }) entries: readonly QueueEntry[] = [];
  @Input({ required: true }) checkedIds: ReadonlySet<string> = new Set();
  @Input({ required: true }) pendingIds: ReadonlySet<string> = new Set();
  @Input() selectedId: string | null = null;

  @Output() readonly select = new EventEmitter<string>();
  @Output() readonly toggle = new EventEmitter<string>();

  alter(iso: string): string {
    const created = new Date(iso).getTime();
    if (Number.isNaN(created)) {
      return '';
    }
    const diff = Date.now() - created;
    const tage = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (tage <= 0) {
      return 'heute';
    }
    if (tage === 1) {
      return '1 Tag';
    }
    return `${tage} Tage`;
  }
}
