import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type Severity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'NOT_APPLICABLE';

const FARBKLASSEN: Record<Severity, string> = {
  CRITICAL: 'bg-red-700 text-white',
  HIGH: 'bg-orange-600 text-white',
  MEDIUM: 'bg-amber-500 text-black',
  LOW: 'bg-yellow-300 text-black',
  INFORMATIONAL: 'bg-sky-200 text-black',
  NOT_APPLICABLE: 'bg-zinc-400 text-white'
};

const LABELS: Record<Severity, string> = {
  CRITICAL: 'Kritisch',
  HIGH: 'Hoch',
  MEDIUM: 'Mittel',
  LOW: 'Niedrig',
  INFORMATIONAL: 'Informativ',
  NOT_APPLICABLE: 'Nicht zutreffend'
};

@Component({
  selector: 'ahs-severity-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold uppercase tracking-wide"
      [ngClass]="cssKlasse"
    >
      {{ anzeige }}
    </span>
  `
})
export class SeverityBadgeComponent {
  @Input({ required: true }) severity!: Severity;

  get cssKlasse(): string {
    return FARBKLASSEN[this.severity] ?? 'bg-zinc-300 text-black';
  }

  get anzeige(): string {
    return LABELS[this.severity] ?? this.severity;
  }
}
