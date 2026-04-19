import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { CvesComponent } from './cves.component';
import { CvesService } from '../../core/cves/cves.service';

class FakeCves {
  findPage = jasmine.createSpy('findPage').and.resolveTo({
    items: [],
    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 0
  });
}

function fakeRoute(params: Record<string, string>) {
  return {
    snapshot: { queryParamMap: convertToParamMap(params) }
  };
}

describe('CvesComponent - Iteration 83 Filter-URL', () => {
  it('liest Filter aus queryParams beim Init', async () => {
    TestBed.configureTestingModule({
      imports: [CvesComponent],
      providers: [
        provideRouter([]),
        { provide: CvesService, useClass: FakeCves },
        { provide: ActivatedRoute, useValue: fakeRoute({
          q: 'apache',
          severity: 'HIGH',
          kev: 'true',
          page: '2'
        }) }
      ]
    });
    const fixture = TestBed.createComponent(CvesComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const c = fixture.componentInstance;
    expect(c.searchText).toBe('apache');
    expect(c.severityFilter).toBe('HIGH');
    expect(c.onlyKev).toBeTrue();
    expect(c.pageIndex).toBe(2);
  });

  it('setSeverity navigiert mit neuem Severity-queryParam', async () => {
    TestBed.configureTestingModule({
      imports: [CvesComponent],
      providers: [
        provideRouter([]),
        { provide: CvesService, useClass: FakeCves },
        { provide: ActivatedRoute, useValue: fakeRoute({}) }
      ]
    });
    const router = TestBed.inject(Router);
    const spy = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(CvesComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    spy.calls.reset();
    fixture.componentInstance.setSeverity('CRITICAL');
    await fixture.whenStable();
    expect(spy).toHaveBeenCalled();
    const params = (spy.calls.mostRecent().args[1] as {
      queryParams: Record<string, unknown>
    }).queryParams;
    expect(params['severity']).toBe('CRITICAL');
  });
});
