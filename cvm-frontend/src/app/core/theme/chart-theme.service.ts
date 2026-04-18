import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject } from '@angular/core';
import { Severity } from '../../shared/components/severity-badge.component';
import { ThemeService } from './theme.service';

/**
 * Liest die aktuellen Theme-Tokens aus dem
 * {@code documentElement} und liefert sie als ECharts-lesbare
 * Werte (Iteration 27c, CVM-63).
 *
 * <p>ECharts kann CSS-Custom-Properties nicht direkt konsumieren;
 * Komponenten holen ihre Farben ueber {@link #severityColors} und
 * {@link #sliceBorderColor} und reagieren ueber das Signal
 * {@link ThemeService#branding} auf Branding-Wechsel.
 */
@Injectable({ providedIn: 'root' })
export class ChartThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ThemeService);

  /**
   * Aktuelle Severity-Farben als Map. Recomputed, sobald das
   * Branding-Signal triggert.
   */
  readonly severityColors = computed<Record<Severity, string>>(() => {
    void this.theme.branding();
    return {
      CRITICAL: this.css('--color-severity-critical-bg', '#da1e28'),
      HIGH: this.css('--color-severity-high-bg', '#ff9868'),
      MEDIUM: this.css('--color-severity-medium-bg', '#f1c21b'),
      LOW: this.css('--color-severity-low-bg', '#28dcaa'),
      INFORMATIONAL: this.css('--color-severity-informational-bg', '#006ec7'),
      NOT_APPLICABLE: this.css('--color-severity-not-applicable-bg', '#c6c6c6')
    };
  });

  /** Border-Farbe zwischen Pie-Slices - Surface des Themes. */
  readonly sliceBorderColor = computed<string>(() => {
    void this.theme.branding();
    return this.css('--color-surface', '#ffffff');
  });

  /** Text-Farbe fuer Achsen und Legenden. */
  readonly textColor = computed<string>(() => {
    void this.theme.branding();
    return this.css('--color-text-muted', '#525252');
  });

  private css(tokenName: string, fallback: string): string {
    try {
      const value = this.document.defaultView
        ?.getComputedStyle(this.document.documentElement)
        .getPropertyValue(tokenName);
      return value && value.trim().length > 0 ? value.trim() : fallback;
    } catch {
      return fallback;
    }
  }
}
