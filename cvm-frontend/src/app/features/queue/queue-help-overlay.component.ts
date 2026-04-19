import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';

/**
 * Hilfeoverlay fuer Queue-Shortcuts. Wird eingeblendet, solange
 * {@link visible} {@code true} ist.
 *
 * Iteration 61 (CVM-62): Nutzt `cvm-dialog` statt eigenes Overlay.
 */
@Component({
  selector: 'cvm-queue-help-overlay',
  standalone: true,
  imports: [CommonModule, CvmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="visible"
      title="Tastatur-Shortcuts"
      size="sm"
      (close)="close.emit()"
    >
      <dl class="grid grid-cols-[auto,1fr] gap-x-4 gap-y-2 text-sm">
        <dt class="font-mono text-text-muted">j / k</dt>
        <dd>naechster / vorheriger Eintrag</dd>
        <dt class="font-mono text-text-muted">a</dt>
        <dd>Approve</dd>
        <dt class="font-mono text-text-muted">o</dt>
        <dd>Override</dd>
        <dt class="font-mono text-text-muted">r</dt>
        <dd>Reject (Kommentar erforderlich)</dd>
        <dt class="font-mono text-text-muted">?</dt>
        <dd>diese Hilfe</dd>
      </dl>
      <div footer>
        <button
          type="button"
          class="btn btn-secondary"
          (click)="close.emit()"
        >
          Schliessen
        </button>
      </div>
    </cvm-dialog>
  `
})
export class QueueHelpOverlayComponent {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();
}
