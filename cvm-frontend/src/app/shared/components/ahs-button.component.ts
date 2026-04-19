import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

type Variante = 'primary' | 'secondary' | 'danger' | 'ghost';

/**
 * Iteration 61A (CVM-62): Pure Tailwind. `mat-flat-button` entfaellt.
 * Ruft `.btn .btn-*`-Klassen aus `styles.scss` auf.
 */
@Component({
  selector: 'ahs-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      type="button"
      class="btn"
      [class.btn-primary]="variant === 'primary'"
      [class.btn-secondary]="variant === 'secondary'"
      [class.btn-danger]="variant === 'danger'"
      [class.btn-ghost]="variant === 'ghost'"
      [disabled]="disabled"
      (click)="ausgeloest.emit($event)"
    >
      <ng-content></ng-content>
    </button>
  `
})
export class AhsButtonComponent {
  @Input() variant: Variante = 'primary';
  @Input() disabled = false;

  @Output() readonly ausgeloest = new EventEmitter<MouseEvent>();
}
