import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { TenantKpiComponent } from './tenant-kpi.component';
import { KpiService } from '../../core/kpi/kpi.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';

class FakeKpi {
  compute = jasmine.createSpy('compute').and.resolveTo({
    window: '90d',
    openBySeverity: {},
    mttrDaysBySeverity: {},
    slaBySeverity: {},
    burnDown: []
  });
}

function fakeRoute(params: Record<string, string>) {
  return {
    snapshot: { queryParamMap: convertToParamMap(params) }
  };
}

describe('TenantKpiComponent - Iteration 83 Window-URL', () => {
  it('liest window aus queryParams beim Init', async () => {
    TestBed.configureTestingModule({
      imports: [TenantKpiComponent],
      providers: [
        provideRouter([]),
        { provide: KpiService, useClass: FakeKpi },
        ChartThemeService,
        { provide: ActivatedRoute, useValue: fakeRoute({ window: '180d' }) }
      ]
    });
    const fixture = TestBed.createComponent(TenantKpiComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.window()).toBe('180d');
  });

  it('fensterWechseln navigiert mit neuem window-Param', async () => {
    TestBed.configureTestingModule({
      imports: [TenantKpiComponent],
      providers: [
        provideRouter([]),
        { provide: KpiService, useClass: FakeKpi },
        ChartThemeService,
        { provide: ActivatedRoute, useValue: fakeRoute({}) }
      ]
    });
    const router = TestBed.inject(Router);
    const spy = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(TenantKpiComponent);
    fixture.detectChanges();
    spy.calls.reset();
    fixture.componentInstance.fensterWechseln('180d');
    const params = (spy.calls.mostRecent().args[1] as {
      queryParams: Record<string, unknown>
    }).queryParams;
    expect(params['window']).toBe('180d');
  });
});
