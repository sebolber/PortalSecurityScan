/**
 * Branding-Konfiguration wie vom Backend ueber
 * {@code GET /api/v1/theme} geliefert (Iteration 27, CVM-61).
 *
 * <p>Alle Werte sind optional in dem Sinn, dass das Backend
 * leere Felder als {@code null} liefert. Der {@link ThemeService}
 * faellt in diesen Faellen auf seinen Default-Datensatz zurueck.
 */
export interface BrandingConfig {
  primaryColor: string;
  primaryContrastColor: string;
  accentColor: string | null;
  fontFamilyName: string;
  fontFamilyMonoName: string | null;
  appTitle: string | null;
  logoUrl: string | null;
  logoAltText: string | null;
  faviconUrl: string | null;
  fontFamilyHref: string | null;
  version: number;
}

export const DEFAULT_BRANDING: BrandingConfig = {
  primaryColor: '#006ec7',
  primaryContrastColor: '#ffffff',
  accentColor: '#887d75',
  fontFamilyName: 'Fira Sans',
  fontFamilyMonoName: 'Fira Code',
  appTitle: 'CVE-Relevance-Manager',
  logoUrl: null,
  logoAltText: 'adesso health solutions',
  faviconUrl: null,
  fontFamilyHref: null,
  version: 1
};

/**
 * Relatives Luminanz-Verhaeltnis (WCAG 2.1 G18). Wir nutzen es
 * frontendseitig, um vor dem Anwenden einer neuen Branding-
 * Konfiguration auf einen erkennbar zu niedrigen Kontrast
 * hinzuweisen. Das Backend erzwingt die Regel ohnehin.
 */
export function contrastRatio(colorA: string, colorB: string): number {
  const la = relativeLuminance(parseHex(colorA));
  const lb = relativeLuminance(parseHex(colorB));
  const bright = Math.max(la, lb);
  const dark = Math.min(la, lb);
  return (bright + 0.05) / (dark + 0.05);
}

export function meetsWcagAa(primaryHex: string, contrastHex: string): boolean {
  return contrastRatio(primaryHex, contrastHex) >= 4.5;
}

function relativeLuminance(rgb: [number, number, number]): number {
  const [r, g, b] = rgb.map((c) => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function parseHex(hex: string): [number, number, number] {
  let value = hex.trim();
  if (value.startsWith('#')) {
    value = value.substring(1);
  }
  if (value.length === 3) {
    value = value
      .split('')
      .map((c) => c + c)
      .join('');
  }
  if (value.length !== 6 || !/^[0-9a-fA-F]{6}$/.test(value)) {
    throw new Error('Ungueltiger Hex-Wert: ' + hex);
  }
  return [
    parseInt(value.substring(0, 2), 16),
    parseInt(value.substring(2, 4), 16),
    parseInt(value.substring(4, 6), 16)
  ];
}
