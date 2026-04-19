import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmDialogComponent } from './cvm-dialog.component';

/**
 * Iteration 91 (CVM-331): Globale Shortcut-Uebersicht. Wird via `?`
 * aus {@link GlobalShortcutsDirective} geoeffnet.
 */
@Component({
  selector: 'cvm-global-shortcuts-overlay',
  standalone: true,
  imports: [CommonModule, CvmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="visible"
      title="Tastatur-Shortcuts"
      size="md"
      (close)="close.emit()"
    >
      <div class="flex flex-col gap-4 text-sm" data-testid="global-shortcuts-sheet">
        <section>
          <h3 class="text-sm font-semibold uppercase text-text-muted mb-2">
            Navigation
          </h3>
          <dl class="grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
            <dt class="font-mono text-text-muted">g d</dt>
            <dd>Start / Dashboard</dd>
            <dt class="font-mono text-text-muted">g q</dt>
            <dd>Bewertungs-Queue</dd>
            <dt class="font-mono text-text-muted">g s</dt>
            <dd>Scan hochladen</dd>
            <dt class="font-mono text-text-muted">g w</dt>
            <dd>Waiver</dd>
            <dt class="font-mono text-text-muted">g r</dt>
            <dd>Berichte</dd>
            <dt class="font-mono text-text-muted">/</dt>
            <dd>Globale Suche</dd>
          </dl>
        </section>

        <section>
          <h3 class="text-sm font-semibold uppercase text-text-muted mb-2">
            Hilfe
          </h3>
          <dl class="grid grid-cols-[auto,1fr] gap-x-4 gap-y-1">
            <dt class="font-mono text-text-muted">?</dt>
            <dd>Diesen Shortcut-Sheet oeffnen</dd>
            <dt class="font-mono text-text-muted">Esc</dt>
            <dd>Dialoge und Overlays schliessen</dd>
          </dl>
        </section>

        <p class="text-xs text-text-muted">
          Hinweis: Shortcuts greifen nur ausserhalb von Eingabefeldern.
          Seitenspezifische Tastatur-Aktionen (z.B. &quot;j&quot;/&quot;k&quot; in der Queue)
          sind im jeweiligen Hilfe-Dialog der Seite dokumentiert.
        </p>
      </div>

      <div footer class="flex items-center justify-end">
        <button type="button" class="btn btn-secondary" (click)="close.emit()">
          Schliessen
        </button>
      </div>
    </cvm-dialog>
  `
})
export class GlobalShortcutsOverlayComponent {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();
}
