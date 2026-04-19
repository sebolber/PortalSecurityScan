import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Platzhalter-Banner fuer Navigationspunkte, deren Inhalt in einer
 * spaeteren Iteration umgesetzt wird. Iteration 61A: Pure Tailwind.
 */
@Component({
  selector: 'cvm-page-placeholder',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  template: `
    <section
      class="flex items-start gap-4 rounded-xl border border-border bg-surface-muted p-6"
      role="status"
      aria-live="polite"
      data-testid="cvm-page-placeholder"
    >
      <cvm-icon name="wrench" [size]="32" class="text-primary shrink-0"></cvm-icon>
      <div class="flex flex-col gap-1">
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
  `
})
export class PagePlaceholderComponent {
  @Input({ required: true }) title!: string;
  @Input({ required: true }) description!: string;
  @Input({ required: true }) iteration!: string;
  @Input() ticket: string | null = null;
}
