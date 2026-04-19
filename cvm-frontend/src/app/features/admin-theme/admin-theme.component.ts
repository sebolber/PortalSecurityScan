import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BrandingConfig,
  DEFAULT_BRANDING,
  contrastRatio,
  meetsWcagAa
} from '../../core/theme/branding';
import {
  BrandingAssetKind,
  BrandingHistoryEntry,
  BrandingHttpService
} from '../../core/theme/branding.service';
import { ThemeService } from '../../core/theme/theme.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';

interface AssetSlot {
  readonly kind: BrandingAssetKind;
  readonly label: string;
  readonly hint: string;
  readonly accept: string;
  readonly fieldKey: 'logoUrl' | 'faviconUrl' | 'fontFamilyHref';
}

const ASSET_SLOTS: readonly AssetSlot[] = [
  {
    kind: 'LOGO',
    label: 'Logo',
    hint: 'SVG oder PNG, max. 512 KB. SVG wird serverseitig auf Scripts/externe Referenzen geprueft.',
    accept: 'image/svg+xml,image/png',
    fieldKey: 'logoUrl'
  },
  {
    kind: 'FAVICON',
    label: 'Favicon',
    hint: 'ICO/PNG/SVG, max. 512 KB.',
    accept: 'image/x-icon,image/png,image/svg+xml,image/vnd.microsoft.icon',
    fieldKey: 'faviconUrl'
  },
  {
    kind: 'FONT',
    label: 'Schrift (woff2)',
    hint: 'Nur woff2, max. 2 MB. Wird als Font-Stylesheet-URL im Branding hinterlegt.',
    accept: 'font/woff2,application/font-woff2',
    fieldKey: 'fontFamilyHref'
  }
];

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
    AhsBannerComponent,
    CvmIconComponent,
    SeverityBadgeComponent
  ],
  templateUrl: './admin-theme.component.html',
  styleUrls: ['./admin-theme.component.scss']
})
export class AdminThemeComponent implements OnInit {
  private readonly branding = inject(BrandingHttpService);
  private readonly theme = inject(ThemeService);
  private readonly toast = inject(CvmToastService);

  readonly assetSlots = ASSET_SLOTS;
  readonly draft = signal<BrandingConfig>({ ...DEFAULT_BRANDING });
  readonly saving = signal(false);
  readonly uploading = signal<Record<BrandingAssetKind, boolean>>({
    LOGO: false,
    FAVICON: false,
    FONT: false
  });
  readonly error = signal<string | null>(null);
  readonly uploadMessage = signal<string | null>(null);
  readonly contrastWarning = this.theme.contrastWarning;

  /**
   * UI-Fix MEDIUM-4 (UI-Exploration 20260418): History-Liste aus dem
   * Backend. Wird beim Laden der Seite geholt und nach jedem Save/
   * Rollback aktualisiert. {@link rollingBackVersion} markiert die
   * Zeile, die gerade zurueckgerollt wird (fuer Button-Disable).
   */
  readonly history = signal<readonly BrandingHistoryEntry[]>([]);
  readonly historyError = signal<string | null>(null);
  readonly rollingBackVersion = signal<number | null>(null);

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
    await this.ladeHistorie();
  }

  private async ladeHistorie(): Promise<void> {
    try {
      const items = await this.branding.history(20);
      this.history.set(items);
      this.historyError.set(null);
    } catch (err) {
      this.history.set([]);
      this.historyError.set(
        err instanceof Error && err.message
          ? err.message
          : 'History konnte nicht geladen werden.'
      );
    }
  }

  async rollback(entry: BrandingHistoryEntry): Promise<void> {
    const bestaetigt = window.confirm(
      'Branding wirklich auf Version ' + entry.version +
        ' zuruecksetzen? Die aktuelle Konfiguration wird als neue Version gespeichert.'
    );
    if (!bestaetigt) {
      return;
    }
    this.rollingBackVersion.set(entry.version);
    this.error.set(null);
    try {
      const saved = await this.branding.rollback(entry.version);
      this.draft.set({ ...saved });
      this.theme.applyBranding(saved);
      this.toast.success(
        'Branding auf Version ' + entry.version + ' zurueckgerollt (neue Version ' +
          saved.version + ').',
        6000
      );
      await this.ladeHistorie();
    } catch (err) {
      this.error.set(
        err instanceof Error && err.message
          ? err.message
          : 'Rollback fehlgeschlagen.'
      );
    } finally {
      this.rollingBackVersion.set(null);
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
      this.toast.success(
        'Branding gespeichert. Jede frueher gespeicherte Version kann '
          + 'ueber die Historie rechts wiederhergestellt werden.',
        6000
      );
      await this.ladeHistorie();
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

  async assetHochladen(slot: AssetSlot, event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file) {
      return;
    }
    this.uploading.update((map) => ({ ...map, [slot.kind]: true }));
    this.error.set(null);
    this.uploadMessage.set(null);
    try {
      const saved = await this.branding.uploadAsset(slot.kind, file);
      this.draft.update((d) => ({ ...d, [slot.fieldKey]: saved.url }));
      this.uploadMessage.set(
        slot.label +
          ' hochgeladen (' +
          Math.round(saved.sizeBytes / 1024) +
          ' KB). URL automatisch ins Formular uebernommen.'
      );
      this.toast.success(slot.label + ' gespeichert', 4000);
    } catch (err) {
      this.error.set(
        err instanceof Error && err.message
          ? err.message
          : slot.label + '-Upload fehlgeschlagen.'
      );
    } finally {
      this.uploading.update((map) => ({ ...map, [slot.kind]: false }));
      input.value = '';
    }
  }
}
