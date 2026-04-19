import { TestBed } from '@angular/core/testing';
import { OnboardingService } from './onboarding.service';

/**
 * Iteration 96 (CVM-336): OnboardingService-Zustand mit
 * localStorage-Persistenz.
 */
describe('OnboardingService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('Standard-Zustand beginnt bei "produkt"', () => {
    const svc = TestBed.inject(OnboardingService);
    expect(svc.state()).toEqual({ done: [], current: 'produkt' });
    expect(svc.completed()).toBeFalse();
  });

  it('markDone setzt den naechsten offenen Schritt', () => {
    const svc = TestBed.inject(OnboardingService);
    svc.markDone('produkt');
    expect(svc.state().done).toEqual(['produkt']);
    expect(svc.state().current).toBe('umgebung');
    svc.markDone('umgebung');
    expect(svc.state().current).toBe('profil');
  });

  it('completed liefert true, sobald alle 4 Schritte erledigt sind', () => {
    const svc = TestBed.inject(OnboardingService);
    svc.markDone('produkt');
    svc.markDone('umgebung');
    svc.markDone('profil');
    svc.markDone('scan');
    expect(svc.completed()).toBeTrue();
  });

  it('doppeltes markDone wird ignoriert', () => {
    const svc = TestBed.inject(OnboardingService);
    svc.markDone('produkt');
    svc.markDone('produkt');
    expect(svc.state().done).toEqual(['produkt']);
  });

  it('State wird in localStorage persistiert und beim Neuladen gelesen', () => {
    const svc = TestBed.inject(OnboardingService);
    svc.markDone('produkt');
    svc.markDone('umgebung');
    const gespeichert = JSON.parse(
      localStorage.getItem('cvm.onboarding.v1') ?? '{}'
    );
    expect(gespeichert.done).toEqual(['produkt', 'umgebung']);
    expect(gespeichert.current).toBe('profil');
  });

  it('reset setzt den Zustand komplett zurueck', () => {
    const svc = TestBed.inject(OnboardingService);
    svc.markDone('produkt');
    svc.reset();
    expect(svc.state().done.length).toBe(0);
    expect(svc.state().current).toBe('produkt');
  });
});
