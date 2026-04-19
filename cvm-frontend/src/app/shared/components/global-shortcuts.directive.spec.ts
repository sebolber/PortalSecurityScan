import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { GlobalShortcutsDirective } from './global-shortcuts.directive';

@Component({
  standalone: true,
  imports: [GlobalShortcutsDirective],
  template: `<div cvmGlobalShortcuts
                   (help)="onHelp()"
                   (search)="onSearch()"></div>`
})
class HostComponent {
  helpCount = 0;
  searchCount = 0;
  onHelp(): void {
    this.helpCount++;
  }
  onSearch(): void {
    this.searchCount++;
  }
}

/**
 * Iteration 91 (CVM-331): GlobalShortcutsDirective.
 */
describe('GlobalShortcutsDirective', () => {
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true));
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideRouter([]), { provide: Router, useValue: router }]
    });
  });

  function dispatch(key: string, options: Partial<KeyboardEventInit> = {}): void {
    document.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, ...options }));
  }

  it('? emittiert help', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('?');
    expect(fixture.componentInstance.helpCount).toBe(1);
  });

  it('g d navigiert zum Dashboard', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('g');
    dispatch('d');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('g q navigiert zur Queue', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('g');
    dispatch('q');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/queue');
  });

  it('g s navigiert zum Scan-Upload', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('g');
    dispatch('s');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/scans/upload');
  });

  it('g w navigiert zu Waivers', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('g');
    dispatch('w');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/waivers');
  });

  it('Shortcuts greifen nicht, wenn der Fokus in einem INPUT liegt', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const input = document.createElement('input');
    document.body.appendChild(input);
    input.focus();
    input.dispatchEvent(new KeyboardEvent('keydown', { key: '?', bubbles: true }));
    expect(fixture.componentInstance.helpCount).toBe(0);
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', bubbles: true }));
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'd', bubbles: true }));
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    input.remove();
  });

  it('Iteration 92: / emittiert search', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('/');
    expect(fixture.componentInstance.searchCount).toBe(1);
  });

  it('g gefolgt von unbekanntem Key navigiert nicht', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    dispatch('g');
    dispatch('x');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
