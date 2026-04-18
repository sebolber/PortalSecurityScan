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
    <table class="min-w-full border-collapse text-sm">
      <thead class="sticky top-0 bg-light text-left text-xs uppercase text-zinc-500">
        <tr>
          <th class="w-10 px-3 py-2">
            <span class="sr-only">Auswahl</span>
          </th>
          <th class="w-32 px-3 py-2">CVE</th>
          <th class="px-3 py-2">Komponente</th>
          <th class="w-24 px-3 py-2">Original</th>
          <th class="w-24 px-3 py-2">Vorschlag</th>
          <th class="w-20 px-3 py-2">Quelle</th>
          <th class="w-20 px-3 py-2">Status</th>
          <th class="w-20 px-3 py-2">Alter</th>
        </tr>
      </thead>
      <tbody>
        @for (eintrag of entries; track eintrag.id) {
          <tr
            class="cursor-pointer border-b last:border-0 hover:bg-zinc-50"
            [class.ausgewaehlt]="eintrag.id === selectedId"
            [class.pending]="pendingIds.has(eintrag.id)"
            (click)="select.emit(eintrag.id)"
            [attr.aria-selected]="eintrag.id === selectedId"
          >
            <td class="px-3 py-2">
              <input
                type="checkbox"
                [checked]="checkedIds.has(eintrag.id)"
                (click)="$event.stopPropagation()"
                (change)="toggle.emit(eintrag.id)"
                [attr.aria-label]="'Auswahl ' + eintrag.cveKey"
              />
            </td>
            <td class="px-3 py-2 font-mono text-xs">{{ eintrag.cveKey }}</td>
            <td class="px-3 py-2">{{ eintrag.rationale || '–' }}</td>
            <td class="px-3 py-2">
              <ahs-severity-badge [severity]="eintrag.severity"></ahs-severity-badge>
            </td>
            <td class="px-3 py-2">
              <ahs-severity-badge [severity]="eintrag.severity"></ahs-severity-badge>
            </td>
            <td class="px-3 py-2 text-xs uppercase tracking-wide">
              {{ eintrag.source }}
            </td>
            <td class="px-3 py-2 text-xs uppercase tracking-wide">
              {{ eintrag.status }}
            </td>
            <td class="px-3 py-2 text-xs text-zinc-500">
              {{ alter(eintrag.createdAt) }}
            </td>
          </tr>
        } @empty {
          <tr>
            <td colspan="8" class="px-3 py-8 text-center text-sm text-zinc-500">
              Keine offenen Vorschlaege fuer die aktuellen Filter.
            </td>
          </tr>
        }
      </tbody>
    </table>
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
