import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type AhsBannerKind = 'info' | 'warn' | 'critical' | 'success';

const ICON: Readonly<Record<AhsBannerKind, string>> = {
  info: 'info',
  warn: 'warning',
  critical: 'report',
  success: 'check_circle'
};

/**
 * Banner-Komponente fuer Eskalationen, Kostenwarnungen, KI-Anomalie-
 * Hinweise und Erfolgsmeldungen (Iteration 27, CVM-61).
 */
@Component({
  selector: 'ahs-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div
      class="ahs-banner"
      [attr.data-kind]="kind"
      role="status"
      aria-live="polite"
    >
      <mat-icon aria-hidden="true">{{ iconName }}</mat-icon>
      <div class="ahs-banner-body">
        @if (title) {
          <strong>{{ title }}</strong>
        }
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .ahs-banner {
        display: flex;
        gap: var(--space-3);
        padding: var(--space-3) var(--space-4);
        border-radius: var(--radius-md);
        border: 1px solid transparent;
      }
      .ahs-banner[data-kind='info'] {
        background: var(--color-banner-info-bg);
        color: var(--color-banner-info-fg);
      }
      .ahs-banner[data-kind='warn'] {
        background: var(--color-banner-warn-bg);
        color: var(--color-banner-warn-fg);
      }
      .ahs-banner[data-kind='critical'] {
        background: var(--color-banner-critical-bg);
        color: var(--color-banner-critical-fg);
      }
      .ahs-banner[data-kind='success'] {
        background: var(--color-banner-success-bg);
        color: var(--color-banner-success-fg);
      }
      .ahs-banner-body {
        display: flex;
        flex-direction: column;
        gap: var(--space-1);
      }
    `
  ]
})
export class AhsBannerComponent {
  @Input() kind: AhsBannerKind = 'info';
  @Input() title: string | null = null;

  get iconName(): string {
    return ICON[this.kind];
  }
}
