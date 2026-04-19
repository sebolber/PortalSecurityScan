import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AnomalyComponent } from './anomaly.component';
import { AnomalyService } from '../../core/anomaly/anomaly.service';

class FakeApi {
  async list(): Promise<never[]> { return []; }
}

describe('AnomalyComponent - Iteration 81 Workflow-CTA', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AnomalyComponent],
      providers: [
        provideRouter([]),
        { provide: AnomalyService, useClass: FakeApi }
      ]
    });
  });

  it('hat einen "Zur Waiver-Verwaltung"-Header-Button', () => {
    const fixture = TestBed.createComponent(AnomalyComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="anomaly-to-waivers"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/waivers');
  });
});
