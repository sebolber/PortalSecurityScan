import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Hilfeoverlay fuer Queue-Shortcuts. Wird eingeblendet, solange
 * {@link visible} {@code true} ist.
 */
@Component({
  selector: 'cvm-queue-help-overlay',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
        role="dialog"
        aria-modal="true"
        aria-label="Tastatur-Shortcuts"
        (click)="close.emit()"
      >
        <div
          class="w-full max-w-md rounded bg-white p-5 shadow-xl"
          (click)="$event.stopPropagation()"
        >
          <h2 class="mb-3 text-lg font-semibold">Tastatur-Shortcuts</h2>
          <dl class="grid grid-cols-[auto,1fr] gap-x-4 gap-y-2 text-sm">
            <dt class="font-mono text-zinc-700">j / k</dt>
            <dd>naechster / vorheriger Eintrag</dd>
            <dt class="font-mono text-zinc-700">a</dt>
            <dd>Approve</dd>
            <dt class="font-mono text-zinc-700">o</dt>
            <dd>Override</dd>
            <dt class="font-mono text-zinc-700">r</dt>
            <dd>Reject (Kommentar erforderlich)</dd>
            <dt class="font-mono text-zinc-700">?</dt>
            <dd>diese Hilfe</dd>
          </dl>
          <div class="mt-4 text-right">
            <button
              type="button"
              class="rounded border border-zinc-300 px-3 py-1 text-sm"
              (click)="close.emit()"
            >
              Schliessen
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class QueueHelpOverlayComponent {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();
}
