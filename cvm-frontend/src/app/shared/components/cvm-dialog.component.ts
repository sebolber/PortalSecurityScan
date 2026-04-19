import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 61A (CVM-62): Einheitliche Dialog-Shell. Ersetzt
 * `MatDialog`. Oeffnen ueber `[open]="true"`, Schliessen via `(close)`.
 *
 * Verwendung:
 *   <cvm-dialog [open]="zeigeDialog()" title="Assessment freigeben" size="lg"
 *               (close)="schliessen()">
 *     <!-- Body -->
 *     <div footer>
 *       <button class="btn btn-secondary" (click)="schliessen()">Abbrechen</button>
 *       <button class="btn btn-primary" (click)="freigeben()">Freigeben</button>
 *     </div>
 *   </cvm-dialog>
 */
export type CvmDialogSize = 'sm' | 'md' | 'lg' | 'xl';

const SIZE_PX: Record<CvmDialogSize, string> = {
  sm: '420px',
  md: '560px',
  lg: '720px',
  xl: '960px'
};

@Component({
  selector: 'cvm-dialog',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open) {
      <div class="dialog-overlay" (click)="onOverlayClick($event)" data-testid="cvm-dialog-overlay"></div>
      <div
        class="dialog-panel"
        role="dialog"
        aria-modal="true"
        [attr.aria-labelledby]="labelId"
        [style.--dialog-size]="widthPx"
        #panel
      >
        <header class="dialog-header">
          <h2 class="dialog-title" [id]="labelId">{{ title }}</h2>
          <button
            type="button"
            class="btn-icon"
            (click)="emitClose()"
            [attr.aria-label]="closeLabel"
          >
            <cvm-icon name="close" [size]="20"></cvm-icon>
          </button>
        </header>
        <div class="dialog-body">
          <ng-content></ng-content>
        </div>
        <footer class="dialog-footer">
          <ng-content select="[footer]"></ng-content>
        </footer>
      </div>
    }
  `
})
export class CvmDialogComponent implements AfterViewInit, OnChanges {
  @Input() open = false;
  @Input() title = '';
  @Input() size: CvmDialogSize = 'md';
  @Input() closeOnOverlay = true;
  @Input() closeLabel = 'Dialog schliessen';

  @Output() readonly close = new EventEmitter<void>();

  @ViewChild('panel') panelRef?: ElementRef<HTMLElement>;

  readonly labelId = `cvm-dialog-${Math.random().toString(36).slice(2, 9)}`;

  get widthPx(): string {
    return SIZE_PX[this.size] ?? SIZE_PX.md;
  }

  ngAfterViewInit(): void {
    this.focusFirst();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && changes['open'].currentValue) {
      queueMicrotask(() => this.focusFirst());
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.emitClose();
    }
  }

  onOverlayClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.closeOnOverlay) {
      this.emitClose();
    }
  }

  emitClose(): void {
    this.close.emit();
  }

  private focusFirst(): void {
    const el = this.panelRef?.nativeElement;
    if (!el) return;
    const focusable = el.querySelector<HTMLElement>(
      'input, select, textarea, button:not([aria-label="' + this.closeLabel + '"]), [tabindex]:not([tabindex="-1"])'
    );
    focusable?.focus();
  }
}
