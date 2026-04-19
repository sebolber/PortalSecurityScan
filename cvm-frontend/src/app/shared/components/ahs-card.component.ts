import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Iteration 61A (CVM-62): Pure Tailwind. `MatCard` entfaellt.
 */
@Component({
  selector: 'ahs-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="card">
      <header class="card-header">
        <div class="flex flex-col gap-1">
          <h2 class="card-title">{{ title }}</h2>
          @if (subtitle) {
            <p class="text-caption">{{ subtitle }}</p>
          }
        </div>
      </header>
      <div class="card-body">
        <ng-content></ng-content>
      </div>
    </section>
  `
})
export class AhsCardComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle: string | null = null;
}
