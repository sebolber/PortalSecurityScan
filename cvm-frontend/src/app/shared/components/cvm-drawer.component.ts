import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 61A (CVM-62): Rechts-Overlay-Drawer fuer Detail-Panels.
 */
@Component({
  selector: 'cvm-drawer',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open) {
      <div class="dialog-overlay" (click)="onOverlayClick()"></div>
      <aside
        class="drawer-panel"
        role="dialog"
        aria-modal="true"
        [style.width]="widthCss"
      >
        <header class="dialog-header">
          <h2 class="dialog-title">{{ title }}</h2>
          <button
            type="button"
            class="btn-icon"
            (click)="emitClose()"
            aria-label="Drawer schliessen"
          >
            <cvm-icon name="close" [size]="20"></cvm-icon>
          </button>
        </header>
        <div class="dialog-body w-full">
          <ng-content></ng-content>
        </div>
        <footer class="dialog-footer">
          <ng-content select="[footer]"></ng-content>
        </footer>
      </aside>
    }
  `
})
export class CvmDrawerComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() width: string | number = '720px';

  @Output() readonly close = new EventEmitter<void>();

  get widthCss(): string {
    const w = this.width;
    return typeof w === 'number' ? `${w}px` : w;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) this.emitClose();
  }

  onOverlayClick(): void {
    this.emitClose();
  }

  emitClose(): void {
    this.close.emit();
  }
}
