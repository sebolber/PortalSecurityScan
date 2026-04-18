import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  BrandingConfig,
  DEFAULT_BRANDING,
  contrastRatio,
  meetsWcagAa
} from '../../core/theme/branding';
import { BrandingHttpService } from '../../core/theme/branding.service';
import { ThemeService } from '../../core/theme/theme.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';

/**
 * Admin-Oberflaeche fuer mandantenspezifisches Branding
 * (Iteration 27, CVM-61).
 *
 * <p>Linker Bereich: Formular mit Farb-, Schrift- und Logo-
 * Eingaben. Rechter Bereich: Live-Vorschau, die die Eingabewerte
 * direkt (ohne Save) im Kontext einer Beispielseite darstellt.
 */
@Component({
  selector: 'cvm-admin-theme',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    AhsBannerComponent,
    SeverityBadgeComponent
  ],
  templateUrl: './admin-theme.component.html',
  styleUrls: ['./admin-theme.component.scss']
})
export class AdminThemeComponent implements OnInit {
  private readonly branding = inject(BrandingHttpService);
  private readonly theme = inject(ThemeService);
  private readonly snackBar = inject(MatSnackBar);

  readonly draft = signal<BrandingConfig>({ ...DEFAULT_BRANDING });
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly contrastWarning = this.theme.contrastWarning;

  readonly contrast = computed(() => {
    const d = this.draft();
    try {
      return contrastRatio(d.primaryColor, d.primaryContrastColor);
    } catch {
      return 0;
    }
  });

  readonly contrastLabel = computed(() => {
    const r = this.contrast();
    if (r >= 7) return 'AAA (' + r.toFixed(2) + ':1)';
    if (r >= 4.5) return 'AA (' + r.toFixed(2) + ':1)';
    return 'failing (' + r.toFixed(2) + ':1)';
  });

  readonly aaOk = computed(() => {
    const d = this.draft();
    try {
      return meetsWcagAa(d.primaryColor, d.primaryContrastColor);
    } catch {
      return false;
    }
  });

  async ngOnInit(): Promise<void> {
    try {
      const current = await this.branding.load();
      this.draft.set({ ...current });
      this.theme.applyBranding(current);
    } catch {
      this.error.set('Branding-Konfiguration konnte nicht geladen werden.');
    }
  }

  update<K extends keyof BrandingConfig>(key: K, value: BrandingConfig[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  preview(): void {
    this.theme.applyBranding(this.draft());
  }

  async save(): Promise<void> {
    if (!this.aaOk()) {
      this.error.set('Kontrastverhaeltnis unter WCAG AA (4.5:1). Bitte anpassen.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      const saved = await this.branding.save({
        primaryColor: this.draft().primaryColor,
        primaryContrastColor: this.draft().primaryContrastColor,
        accentColor: this.draft().accentColor,
        fontFamilyName: this.draft().fontFamilyName,
        fontFamilyMonoName: this.draft().fontFamilyMonoName,
        appTitle: this.draft().appTitle,
        logoUrl: this.draft().logoUrl,
        logoAltText: this.draft().logoAltText,
        faviconUrl: this.draft().faviconUrl,
        fontFamilyHref: this.draft().fontFamilyHref,
        expectedVersion: this.draft().version
      });
      this.draft.set({ ...saved });
      this.theme.applyBranding(saved);
      this.snackBar.open(
        'Branding gespeichert. Rollback innerhalb 24 h moeglich.',
        'OK',
        { duration: 6000 }
      );
    } catch (err) {
      this.error.set(
        err instanceof Error && err.message
          ? err.message
          : 'Speichern fehlgeschlagen.'
      );
    } finally {
      this.saving.set(false);
    }
  }

  reset(): void {
    this.draft.set({ ...DEFAULT_BRANDING });
    this.theme.applyBranding(DEFAULT_BRANDING);
  }
}
