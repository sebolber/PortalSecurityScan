import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FixVerificationComponent } from './fix-verification.component';
import { FixVerificationQueryHttpService } from '../../core/fix-verification/fix-verification.service';

class FakeApi {
  async list(): Promise<never[]> { return []; }
}

describe('FixVerificationComponent - Iteration 81 Workflow-CTA', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FixVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: FixVerificationQueryHttpService, useClass: FakeApi }
      ]
    });
  });

  it('hat einen "Zur Waiver-Verwaltung"-Header-Button', () => {
    const fixture = TestBed.createComponent(FixVerificationComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="fix-verification-to-waivers"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/waivers');
  });
});
