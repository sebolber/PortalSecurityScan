import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OnboardingComponent } from './onboarding.component';
import { OnboardingService } from './onboarding.service';

describe('OnboardingComponent - Iteration 96', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [OnboardingComponent],
      providers: [provideRouter([])]
    });
  });

  afterEach(() => localStorage.clear());

  it('rendert 4 Schritte, Schritt 1 als aktuell markiert', () => {
    const fixture = TestBed.createComponent(OnboardingComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="onboarding-step-produkt"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="onboarding-step-scan"]'))
      .not.toBeNull();
    expect(fixture.componentInstance.schritte()[0].current).toBeTrue();
    expect(fixture.componentInstance.schritte()[0].done).toBeFalse();
  });

  it('markiereErledigt setzt den Schritt als done und springt zum naechsten', () => {
    const fixture = TestBed.createComponent(OnboardingComponent);
    fixture.detectChanges();
    fixture.componentInstance.markiereErledigt('produkt');
    fixture.detectChanges();
    const schritte = fixture.componentInstance.schritte();
    expect(schritte[0].done).toBeTrue();
    expect(schritte[1].current).toBeTrue();
  });

  it('Fortschritt 25% nach einem Schritt, 100% nach vier', () => {
    const fixture = TestBed.createComponent(OnboardingComponent);
    fixture.detectChanges();
    fixture.componentInstance.markiereErledigt('produkt');
    expect(fixture.componentInstance.fortschritt()).toBe(25);
    fixture.componentInstance.markiereErledigt('umgebung');
    fixture.componentInstance.markiereErledigt('profil');
    fixture.componentInstance.markiereErledigt('scan');
    expect(fixture.componentInstance.fortschritt()).toBe(100);
    expect(fixture.componentInstance.fertig()).toBeTrue();
  });

  it('zuruecksetzen setzt den Zustand zurueck', () => {
    const service = TestBed.inject(OnboardingService);
    const fixture = TestBed.createComponent(OnboardingComponent);
    service.markDone('produkt');
    fixture.detectChanges();
    fixture.componentInstance.zuruecksetzen();
    expect(service.state().done.length).toBe(0);
  });
});
