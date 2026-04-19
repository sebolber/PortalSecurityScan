import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { FixVerificationComponent } from './fix-verification.component';
import { FixVerificationQueryHttpService } from '../../core/fix-verification/fix-verification.service';

class FakeApi {
  async list(): Promise<never[]> { return []; }
}

function fakeRoute(params: Record<string, string>) {
  return {
    snapshot: { queryParamMap: convertToParamMap(params) }
  };
}

describe('FixVerificationComponent - Iteration 81+83', () => {
  it('Iteration 81: hat einen "Zur Waiver-Verwaltung"-Header-Button', () => {
    TestBed.configureTestingModule({
      imports: [FixVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: FixVerificationQueryHttpService, useClass: FakeApi },
        { provide: ActivatedRoute, useValue: fakeRoute({}) }
      ]
    });
    const fixture = TestBed.createComponent(FixVerificationComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="fix-verification-to-waivers"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/waivers');
  });

  it('Iteration 83: liest grade aus queryParams beim Init', () => {
    TestBed.configureTestingModule({
      imports: [FixVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: FixVerificationQueryHttpService, useClass: FakeApi },
        { provide: ActivatedRoute, useValue: fakeRoute({ grade: 'B' }) }
      ]
    });
    const fixture = TestBed.createComponent(FixVerificationComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.grade()).toBe('B');
  });

  it('Iteration 83: gradeWechseln navigiert mit neuem Grade', () => {
    TestBed.configureTestingModule({
      imports: [FixVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: FixVerificationQueryHttpService, useClass: FakeApi },
        { provide: ActivatedRoute, useValue: fakeRoute({}) }
      ]
    });
    const router = TestBed.inject(Router);
    const spy = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(FixVerificationComponent);
    fixture.detectChanges();
    spy.calls.reset();
    fixture.componentInstance.gradeWechseln('C');
    const params = (spy.calls.mostRecent().args[1] as {
      queryParams: Record<string, unknown>
    }).queryParams;
    expect(params['grade']).toBe('C');
  });
});
