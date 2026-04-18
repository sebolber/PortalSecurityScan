import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { ChartThemeService } from './chart-theme.service';
import { DEFAULT_BRANDING } from './branding';
import { ThemeService } from './theme.service';

describe('ChartThemeService', () => {
  let doc: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.style.cssText = '';
  });

  afterEach(() => {
    doc.documentElement.style.cssText = '';
  });

  it('liefert Severity-Fallbacks wenn keine Tokens gesetzt sind', () => {
    const service = TestBed.inject(ChartThemeService);
    const colors = service.severityColors();
    expect(colors.CRITICAL).toMatch(/^#?[0-9a-fA-F]{3,}/);
    expect(colors.HIGH).toBeTruthy();
    expect(colors.LOW).toBeTruthy();
  });

  it('liest --color-severity-critical-bg aus dem documentElement', () => {
    doc.documentElement.style.setProperty(
      '--color-severity-critical-bg',
      '#ff0000'
    );
    const service = TestBed.inject(ChartThemeService);
    expect(service.severityColors().CRITICAL).toBe('#ff0000');
  });

  it('aktualisiert sich, wenn ThemeService.applyBranding laeuft', () => {
    const theme = TestBed.inject(ThemeService);
    const service = TestBed.inject(ChartThemeService);
    theme.applyBranding(DEFAULT_BRANDING);

    doc.documentElement.style.setProperty('--color-surface', '#123456');
    // Re-apply triggers branding signal, compute() revaluiert.
    theme.applyBranding(DEFAULT_BRANDING);
    expect(service.sliceBorderColor()).toBe('#123456');
  });
});
