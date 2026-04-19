import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WaiversComponent } from './waivers.component';
import { WaiversService } from '../../core/waivers/waivers.service';

class FakeApi {
  async list(): Promise<never[]> { return []; }
}

describe('WaiversComponent - Iteration 81 Workflow-CTA', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WaiversComponent],
      providers: [
        provideRouter([]),
        { provide: WaiversService, useClass: FakeApi }
      ]
    });
  });

  it('hat einen "Zum Hardening-Bericht"-Header-Button', () => {
    const fixture = TestBed.createComponent(WaiversComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="waivers-to-reports"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/reports');
  });
});
