import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { BrandingConfig, DEFAULT_BRANDING, meetsWcagAa } from './branding';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'cvm.theme';
const DATA_ATTRIBUTE = 'data-theme';

/**
 * Light/Dark-Umschalter (Iteration 24) + Laufzeit-Branding
 * (Iteration 27, CVM-61).
 *
 * <p>Beim Start liest der Service das persistierte Light/Dark-
 * Flag aus dem {@code localStorage} und setzt es als
 * {@code data-theme}-Attribut am {@code <html>}-Element. Die
 * Branding-Konfiguration (Primaerfarbe, Schrift, Logo, Titel)
 * kommt asynchron vom Backend via {@link #applyBranding}. Wird
 * ein kontrastarmes Farbpaar uebergeben, faellt der Service auf
 * das Default-Branding zurueck und setzt ein Warn-Flag, das die
 * Admin-Oberflaeche anzeigen kann.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storage: Storage | null = this.resolveStorage();

  readonly mode = signal<ThemeMode>(this.resolveInitialMode());
  readonly branding = signal<BrandingConfig>({ ...DEFAULT_BRANDING });
  readonly contrastWarning = signal<string | null>(null);

  init(): void {
    this.apply(this.mode());
    this.applyBranding(DEFAULT_BRANDING);
  }

  set(mode: ThemeMode): void {
    this.mode.set(mode);
    this.apply(mode);
  }

  toggle(): ThemeMode {
    const next: ThemeMode = this.mode() === 'dark' ? 'light' : 'dark';
    this.set(next);
    return next;
  }

  /**
   * Uebernimmt eine neue Branding-Konfiguration. Wird die
   * WCAG-AA-Schwelle unterschritten, bleibt das zuletzt gueltige
   * Branding erhalten und {@link #contrastWarning} wird gesetzt.
   */
  applyBranding(config: BrandingConfig): void {
    const validated = this.validate(config);
    if (validated === null) {
      this.contrastWarning.set(
        'Kontrast zwischen Primaerfarbe und Textfarbe liegt unter WCAG AA - Default bleibt aktiv.'
      );
      return;
    }
    this.contrastWarning.set(null);
    this.branding.set(validated);
    this.writeCssVariables(validated);
    this.writeDocumentMeta(validated);
  }

  resetBranding(): void {
    this.applyBranding({ ...DEFAULT_BRANDING });
  }

  private validate(config: BrandingConfig): BrandingConfig | null {
    try {
      if (!meetsWcagAa(config.primaryColor, config.primaryContrastColor)) {
        return null;
      }
    } catch {
      return null;
    }
    return config;
  }

  private writeCssVariables(config: BrandingConfig): void {
    const root = this.document.documentElement;
    if (!root) {
      return;
    }
    root.style.setProperty('--color-primary', config.primaryColor);
    root.style.setProperty('--color-primary-contrast', config.primaryContrastColor);
    if (config.accentColor) {
      root.style.setProperty('--color-accent', config.accentColor);
    }
    root.style.setProperty(
      '--font-family-sans',
      `'${config.fontFamilyName}', 'Fira Sans', 'Inter', sans-serif`
    );
    if (config.fontFamilyMonoName) {
      root.style.setProperty(
        '--font-family-mono',
        `'${config.fontFamilyMonoName}', 'Fira Code', monospace`
      );
    }
  }

  private writeDocumentMeta(config: BrandingConfig): void {
    const doc = this.document;
    if (config.appTitle && doc.title !== config.appTitle) {
      doc.title = config.appTitle;
    }
    if (config.faviconUrl) {
      let link = doc.querySelector<HTMLLinkElement>("link[rel='icon']");
      if (!link) {
        link = doc.createElement('link');
        link.rel = 'icon';
        doc.head.appendChild(link);
      }
      link.href = config.faviconUrl;
    }
    if (config.fontFamilyHref) {
      const fontLinkId = 'cvm-brand-font';
      let fontLink = doc.getElementById(fontLinkId) as HTMLLinkElement | null;
      if (!fontLink) {
        fontLink = doc.createElement('link');
        fontLink.id = fontLinkId;
        fontLink.rel = 'stylesheet';
        doc.head.appendChild(fontLink);
      }
      fontLink.href = config.fontFamilyHref;
    }
  }

  private apply(mode: ThemeMode): void {
    const root = this.document.documentElement;
    if (root) {
      root.setAttribute(DATA_ATTRIBUTE, mode);
    }
    try {
      this.storage?.setItem(STORAGE_KEY, mode);
    } catch {
      // localStorage ggf. verweigert (privater Modus).
    }
  }

  private resolveInitialMode(): ThemeMode {
    const stored = this.readStored();
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
    }
    return 'light';
  }

  private readStored(): string | null {
    try {
      return this.storage?.getItem(STORAGE_KEY) ?? null;
    } catch {
      return null;
    }
  }

  private resolveStorage(): Storage | null {
    try {
      return typeof window !== 'undefined' ? window.localStorage : null;
    } catch {
      return null;
    }
  }
}
