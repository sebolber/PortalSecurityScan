import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Leerzustands-Komponente. Iteration 61A: Pure Tailwind, lucide-Icons.
 */
@Component({
  selector: 'ahs-empty-state',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  template: `
    <div class="flex flex-col items-center justify-center gap-2 py-12 text-text-muted text-center">
      <cvm-icon [name]="icon" [size]="40" class="text-text-muted"></cvm-icon>
      <h3 class="text-title-sm">{{ title }}</h3>
      @if (hint) {
        <p class="text-sm max-w-[40ch]">{{ hint }}</p>
      }
      <ng-content></ng-content>
    </div>
  `
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input({ required: true }) title!: string;
  @Input() hint: string | null = null;
}
