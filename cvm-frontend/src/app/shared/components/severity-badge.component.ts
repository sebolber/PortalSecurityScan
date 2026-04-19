import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export type Severity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'NOT_APPLICABLE';

const LABELS: Record<Severity, string> = {
  CRITICAL: 'Kritisch',
  HIGH: 'Hoch',
  MEDIUM: 'Mittel',
  LOW: 'Niedrig',
  INFORMATIONAL: 'Informativ',
  NOT_APPLICABLE: 'Nicht zutreffend'
};

/**
 * Severity-Badge. Iteration 61A: Pure Tailwind. Mapping auf
 * `.severity-chip[data-sev]` in `styles.scss`.
 */
@Component({
  selector: 'ahs-severity-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="severity-chip" [attr.data-sev]="severitySig()">
      {{ label() }}
    </span>
  `
})
export class SeverityBadgeComponent {
  private readonly _severity = signal<Severity>('INFORMATIONAL');

  @Input({ required: true })
  set severity(v: Severity) {
    this._severity.set(v);
  }
  get severity(): Severity {
    return this._severity();
  }

  readonly severitySig = this._severity.asReadonly();
  readonly label = computed(() => LABELS[this._severity()] ?? this._severity());
}
