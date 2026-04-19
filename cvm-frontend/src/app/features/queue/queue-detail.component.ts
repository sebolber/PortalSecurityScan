import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { QueueEntry, SEVERITY_REIHENFOLGE } from './queue.types';
import { Severity } from '../../shared/components/severity-badge.component';
import { braucheZweitfreigabe } from './vier-augen';

/**
 * Slide-In-Detailpanel. Haelt lokal editierbare Kopien der
 * Severity/Rationale/Mitigation-Felder. Aktionen werden als Events an
 * den Container (Queue-Komponente) weitergereicht; der Container
 * orchestriert den Store-Aufruf.
 */
@Component({
  selector: 'cvm-queue-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SeverityBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="flex h-full w-full max-w-lg flex-col border-l bg-white shadow-xl"
      role="dialog"
      aria-label="Bewertungs-Detail"
      tabindex="-1"
    >
      @if (entry) {
        <header class="flex items-center justify-between gap-2 border-b p-4">
          <div>
            <div class="text-xs uppercase text-zinc-500">
              {{ entry.source }} &middot; v{{ entry.version }}
            </div>
            <a class="font-mono text-sm text-primary hover:underline"
               [routerLink]="['/cves', entry.cveKey]"
               title="Zur CVE-Detailseite">{{ entry.cveKey }}</a>
          </div>
          <button
            type="button"
            class="text-sm text-zinc-500 hover:text-primary"
            (click)="close.emit()"
            aria-label="Schliessen"
          >
            Schliessen
          </button>
        </header>

        <div class="flex-1 space-y-4 overflow-y-auto p-4 text-sm">
          <div>
            <span class="block text-xs uppercase text-zinc-500">Aktueller Status</span>
            <span class="font-medium">{{ entry.status }}</span>
          </div>

          <div>
            <span class="block text-xs uppercase text-zinc-500">Original-Severity</span>
            <ahs-severity-badge [severity]="entry.severity"></ahs-severity-badge>
          </div>

          <label class="block">
            <span class="mb-1 block text-xs uppercase text-zinc-500">
              Vorschlags-Severity (editierbar)
            </span>
            <select
              class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
              [(ngModel)]="zielSeverity"
            >
              @for (s of severities; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
            <span class="mt-1 block text-xs text-zinc-500">
              Aenderung wird beim Klick auf <strong>Freigeben</strong>
              uebernommen. Ein Downgrade auf INFORMATIONAL oder
              NOT_APPLICABLE erfordert Zweitfreigabe.
            </span>
          </label>

          <label class="block">
            <span class="mb-1 block text-xs uppercase text-zinc-500">
              Begruendung
            </span>
            <textarea
              class="h-24 w-full rounded border border-zinc-300 px-2 py-1 text-sm"
              [(ngModel)]="rationale"
              placeholder="Warum wird so entschieden?"
            ></textarea>
          </label>

          <fieldset class="rounded border border-zinc-200 p-3">
            <legend class="px-1 text-xs uppercase text-zinc-500">Mitigation</legend>
            <label class="mb-2 block">
              <span class="mb-1 block text-xs text-zinc-500">Strategie</span>
              <select
                class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
                [(ngModel)]="strategy"
                name="strategy"
              >
                <option value="">(keine Angabe)</option>
                @for (s of mitigationStrategien; track s) {
                  <option [value]="s">{{ s }}</option>
                }
              </select>
            </label>
            <label class="mb-2 block">
              <span class="mb-1 block text-xs text-zinc-500">Ziel-Release</span>
              <input
                type="text"
                class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
                [(ngModel)]="targetVersion"
                placeholder="z.B. 1.15.0"
              />
            </label>
            <label class="mb-2 block">
              <span class="mb-1 block text-xs text-zinc-500">Geplant bis</span>
              <input
                type="date"
                class="w-full rounded border border-zinc-300 px-2 py-1 text-sm"
                [(ngModel)]="plannedFor"
              />
            </label>
          </fieldset>

          @if (zweitfreigabe) {
            <div
              class="rounded border border-amber-400 bg-amber-50 p-3 text-xs text-amber-800"
              role="status"
            >
              Zweitfreigabe erforderlich: Downgrade auf {{ zielSeverity }} darf
              nicht von der Person freigegeben werden, die den Vorschlag angelegt hat.
            </div>
          }

          @if (showRejectKommentar) {
            <label class="block">
              <span class="mb-1 block text-xs uppercase text-zinc-500">
                Ablehnungs-Kommentar (pflicht)
              </span>
              <textarea
                class="h-20 w-full rounded border border-red-400 px-2 py-1 text-sm"
                [(ngModel)]="rejectKommentar"
              ></textarea>
            </label>
          }
        </div>

        <footer class="flex flex-wrap gap-2 border-t p-4">
          <button
            type="button"
            class="rounded bg-primary px-3 py-1 text-sm font-medium text-white disabled:opacity-50"
            [disabled]="pending"
            (click)="onApprove()"
          >
            {{ zweitfreigabe ? 'Zur Zweitfreigabe einreichen' : 'Freigeben' }}
          </button>
          <button
            type="button"
            class="rounded border border-red-400 px-3 py-1 text-sm text-red-700"
            [disabled]="pending"
            (click)="toggleRejectKommentar()"
          >
            Reject
          </button>
          @if (showRejectKommentar) {
            <button
              type="button"
              class="rounded bg-red-600 px-3 py-1 text-sm text-white disabled:opacity-50"
              [disabled]="pending || rejectKommentar.trim().length === 0"
              (click)="onReject()"
            >
              Reject bestaetigen
            </button>
          }
        </footer>
      } @else {
        <div class="flex h-full items-center justify-center p-8 text-sm text-zinc-500">
          Keinen Vorschlag ausgewaehlt. {{ 'j'.toUpperCase() }}/{{ 'k'.toUpperCase() }}
          bewegt durch die Liste.
        </div>
      }
    </section>
  `
})
export class QueueDetailComponent {
  @Input() entry: QueueEntry | null = null;
  @Input() pending = false;

  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly approve = new EventEmitter<{
    severity: Severity;
    rationale: string;
    strategy?: string;
    targetVersion?: string;
    plannedFor?: string;
    zweitfreigabe: boolean;
  }>();
  @Output() readonly reject = new EventEmitter<string>();

  readonly severities = SEVERITY_REIHENFOLGE;

  // Muss synchron zum Backend-Enum bleiben:
  // com.ahs.cvm.domain.enums.MitigationStrategy.
  readonly mitigationStrategien = [
    'UPGRADE',
    'PATCH',
    'CONFIG_CHANGE',
    'WORKAROUND',
    'ACCEPT_RISK',
    'NOT_APPLICABLE'
  ] as const;

  zielSeverity: Severity = 'MEDIUM';
  rationale = '';
  strategy = '';
  targetVersion = '';
  plannedFor = '';
  rejectKommentar = '';
  showRejectKommentar = false;

  ngOnChanges(): void {
    if (this.entry) {
      this.zielSeverity = this.entry.severity;
      this.rationale = this.entry.rationale ?? '';
      this.strategy = '';
      this.targetVersion = '';
      this.plannedFor = '';
      this.rejectKommentar = '';
      this.showRejectKommentar = false;
    }
  }

  get zweitfreigabe(): boolean {
    return braucheZweitfreigabe(this.zielSeverity, this.entry?.severity ?? null);
  }

  onApprove(): void {
    this.approve.emit({
      severity: this.zielSeverity,
      rationale: this.rationale,
      strategy: this.strategy || undefined,
      targetVersion: this.targetVersion || undefined,
      plannedFor: this.plannedFor || undefined,
      zweitfreigabe: this.zweitfreigabe
    });
  }

  toggleRejectKommentar(): void {
    this.showRejectKommentar = !this.showRejectKommentar;
  }

  onReject(): void {
    if (this.rejectKommentar.trim().length === 0) {
      return;
    }
    this.reject.emit(this.rejectKommentar.trim());
  }
}
