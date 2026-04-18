import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let doc: Document;

  beforeEach(() => {
    localStorage.removeItem('cvm.theme');
    TestBed.configureTestingModule({});
    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.removeAttribute('data-theme');
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
});
