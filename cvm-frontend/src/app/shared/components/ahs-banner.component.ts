import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';

export type AhsBannerKind = 'info' | 'warn' | 'critical' | 'success';

const ICON: Readonly<Record<AhsBannerKind, string>> = {
  info: 'info',
  warn: 'alert-triangle',
  critical: 'alert-circle',
  success: 'check-circle'
};

const BANNER_CLASS: Readonly<Record<AhsBannerKind, string>> = {
  info: 'banner-info',
  warn: 'banner-warning',
  critical: 'banner-critical',
  success: 'banner-success'
};

/**
 * Banner-Komponente fuer Eskalationen, Kostenwarnungen, KI-Anomalie-
 * Hinweise und Erfolgsmeldungen. Iteration 61A: Pure Tailwind.
 */
@Component({
  selector: 'ahs-banner',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  template: `
    <div class="banner" [ngClass]="bannerClass" role="status" aria-live="polite">
      <cvm-icon [name]="iconName" [size]="18" aria-hidden="true"></cvm-icon>
      <div class="flex flex-col gap-1 grow">
        @if (title) {
          <strong class="text-sm font-semibold">{{ title }}</strong>
        }
        <div class="text-sm">
          <ng-content></ng-content>
        </div>
      </div>
    </div>
  `
})
export class AhsBannerComponent {
  @Input() kind: AhsBannerKind = 'info';
  @Input() title: string | null = null;

  get iconName(): string {
    return ICON[this.kind];
  }

  get bannerClass(): string {
    return BANNER_CLASS[this.kind];
  }
}
