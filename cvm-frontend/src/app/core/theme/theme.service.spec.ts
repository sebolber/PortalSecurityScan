import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { DEFAULT_BRANDING } from './branding';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let doc: Document;

  beforeEach(() => {
    localStorage.removeItem('cvm.theme');
    TestBed.configureTestingModule({});
    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.removeAttribute('data-theme');
    doc.documentElement.style.cssText = '';
  });

  it('init setzt data-theme-Attribut am <html>', () => {
    const service = TestBed.inject(ThemeService);
    service.init();
    expect(doc.documentElement.getAttribute('data-theme')).toMatch(/light|dark/);
  });

  it('toggle wechselt zwischen light und dark', () => {
    const service = TestBed.inject(ThemeService);
    service.set('light');
    expect(service.mode()).toBe('light');
    expect(service.toggle()).toBe('dark');
    expect(service.mode()).toBe('dark');
    expect(doc.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(service.toggle()).toBe('light');
    expect(doc.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('persistiert gewaehlten Modus in localStorage', () => {
    const service = TestBed.inject(ThemeService);
    service.set('dark');
    expect(localStorage.getItem('cvm.theme')).toBe('dark');
  });

  it('uebernimmt persistenten Modus beim Start', () => {
    localStorage.setItem('cvm.theme', 'dark');
    const service = TestBed.inject(ThemeService);
    expect(service.mode()).toBe('dark');
  });

  it('applyBranding setzt CSS-Custom-Properties auf das html-Element', () => {
    const service = TestBed.inject(ThemeService);
    service.applyBranding({
      ...DEFAULT_BRANDING,
      primaryColor: '#123456',
      primaryContrastColor: '#ffffff',
      fontFamilyName: 'Fira Sans'
    });
    expect(doc.documentElement.style.getPropertyValue('--color-primary'))
      .toBe('#123456');
    expect(doc.documentElement.style.getPropertyValue('--font-family-sans'))
      .toContain('Fira Sans');
    expect(service.contrastWarning()).toBeNull();
  });

  it('applyBranding mit zu geringem Kontrast setzt Warnung und behaelt Default', () => {
    const service = TestBed.inject(ThemeService);
    service.init();
    service.applyBranding({
      ...DEFAULT_BRANDING,
      primaryColor: '#cccccc',
      primaryContrastColor: '#ffffff'
    });
    expect(service.contrastWarning()).toMatch(/Kontrast/);
    expect(service.branding().primaryColor).toBe(DEFAULT_BRANDING.primaryColor);
  });

  it('applyBranding setzt document.title wenn appTitle gesetzt ist', () => {
    const service = TestBed.inject(ThemeService);
    service.applyBranding({
      ...DEFAULT_BRANDING,
      appTitle: 'BKK-Test CVM'
    });
    expect(doc.title).toBe('BKK-Test CVM');
  });
});
