import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'ahs-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="flex flex-col items-center justify-center gap-2 p-8 text-zinc-500">
      <mat-icon class="text-4xl" [style.fontSize.px]="40">{{ icon }}</mat-icon>
      <h3 class="text-lg font-semibold">{{ title }}</h3>
      @if (hint) {
        <p class="max-w-md text-center text-sm">{{ hint }}</p>
      }
    </div>
  `
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input({ required: true }) title!: string;
  @Input() hint: string | null = null;
}
