import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * Platzhalter-Banner fuer Navigationspunkte, deren Inhalt in einer
 * spaeteren Iteration umgesetzt wird (Iteration 27, CVM-61).
 *
 * <p>Sorgt dafuer, dass keine Route in der Sidebar auf eine leere
 * Seite fuehrt. Der {@code FullNavigationWalkThroughTest} in
 * Iteration 27b akzeptiert eine Seite als "nicht stumm", sobald
 * dieses Element mit erkennbarem Hinweistext und Iterations-
 * Referenz sichtbar ist.
 */
@Component({
  selector: 'cvm-page-placeholder',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <section
      class="cvm-page-placeholder"
      role="status"
      aria-live="polite"
      data-testid="cvm-page-placeholder"
    >
      <mat-icon aria-hidden="true">construction</mat-icon>
      <div>
        <h2 class="text-title-md">{{ title }}</h2>
        <p class="text-body">{{ description }}</p>
        <p class="text-caption">
          Wird in {{ iteration }}
          @if (ticket) {
            / {{ ticket }}
          }
          umgesetzt.
        </p>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .cvm-page-placeholder {
        display: flex;
        gap: var(--space-4);
        align-items: flex-start;
        padding: var(--space-6);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        background: var(--color-surface-muted);
        color: var(--color-text);
      }
      .cvm-page-placeholder mat-icon {
        color: var(--color-primary);
        font-size: 32px;
        width: 32px;
        height: 32px;
      }
    `
  ]
})
export class PagePlaceholderComponent {
  @Input({ required: true }) title!: string;
  @Input({ required: true }) description!: string;
  @Input({ required: true }) iteration!: string;
  @Input() ticket: string | null = null;
}
