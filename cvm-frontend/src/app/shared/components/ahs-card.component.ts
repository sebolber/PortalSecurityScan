import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

/**
 * adesso-Variante einer Material-Card. Trennt Titel/Subtitle vom
 * Inhalt und sorgt fuer einheitliches Padding/Hover-Verhalten.
 */
@Component({
  selector: 'ahs-card',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <mat-card class="ahs-card">
      <mat-card-header>
        <mat-card-title>{{ title }}</mat-card-title>
        @if (subtitle) {
          <mat-card-subtitle>{{ subtitle }}</mat-card-subtitle>
        }
      </mat-card-header>
      <mat-card-content>
        <ng-content></ng-content>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      :host { display: block; }
      .ahs-card { transition: box-shadow 200ms ease; }
      .ahs-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
    `
  ]
})
export class AhsCardComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle: string | null = null;
}
