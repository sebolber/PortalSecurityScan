import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Leerzustands-Komponente. Tokens statt Tailwind-Zinc-Arbitrary-
 * Werte (Iteration 27b, CVM-62).
 */
@Component({
  selector: 'ahs-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="ahs-empty-state">
      <mat-icon aria-hidden="true">{{ icon }}</mat-icon>
      <h3 class="text-title-sm">{{ title }}</h3>
      @if (hint) {
        <p class="text-body ahs-empty-state__hint">{{ hint }}</p>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .ahs-empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        padding: var(--space-7);
        color: var(--color-text-muted);
      }
      .ahs-empty-state mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
      }
      .ahs-empty-state__hint {
        max-width: 40ch;
        text-align: center;
      }
    `
  ]
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input({ required: true }) title!: string;
  @Input() hint: string | null = null;
}
