import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

type Variante = 'primary' | 'secondary' | 'danger';

@Component({
  selector: 'ahs-button',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <button
      mat-flat-button
      [color]="materialFarbe"
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

  get materialFarbe(): 'primary' | 'accent' | 'warn' {
    if (this.variant === 'danger') {
      return 'warn';
    }
    if (this.variant === 'secondary') {
      return 'accent';
    }
    return 'primary';
  }
}
