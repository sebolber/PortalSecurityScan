import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertBannerService } from '../core/alerts/alert-banner.service';

/**
 * Rotes Banner oberhalb der Shell, wenn das Backend einen
 * T2-Eskalationsstand meldet.
 */
@Component({
  selector: 'cvm-alert-banner',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (status()?.visible) {
      <div
        class="cvm-alert-banner"
        role="alert"
        aria-live="polite"
      >
        <span class="cvm-alert-banner__symbol" aria-hidden="true">!</span>
        <span>
          T2-Eskalation: {{ status()?.count }} kritische Vorschlaege
          aelter als {{ status()?.t2Minutes }} Minuten ohne Bewertung.
        </span>
      </div>
    }
  `,
  styles: [
    `
      .cvm-alert-banner {
        background-color: #b00020;
        color: #fff;
        padding: 8px 16px;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .cvm-alert-banner__symbol {
        font-weight: bold;
        background-color: rgba(255, 255, 255, 0.15);
        border-radius: 50%;
        width: 22px;
        height: 22px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
    `
  ]
})
export class AlertBannerComponent {
  private readonly bannerService = inject(AlertBannerService);
  readonly status = this.bannerService.status;
}
