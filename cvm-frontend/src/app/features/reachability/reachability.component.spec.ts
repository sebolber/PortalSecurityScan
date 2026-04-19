import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { ReachabilityComponent } from './reachability.component';
import {
  ReachabilityQueryService,
  ReachabilitySummaryView
} from '../../core/reachability/reachability.service';
import { AuthService } from '../../core/auth/auth.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

const ROW: ReachabilitySummaryView = {
  id: 'r1',
  findingId: 'f1',
  status: 'PROPOSED',
  severity: 'HIGH',
  rationale: 'Erreichbar via Servlet.service',
  confidence: 0.92,
  createdAt: '2026-04-19T10:00:00Z'
};

class FakeApi {
  list = jasmine.createSpy('list').and.resolveTo([ROW]);
}
class FakeAuth {
  username = () => 't.tester@ahs.test';
  hasRole = () => true;
  loggedIn = () => true;
  userRoles = () => [];
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> { return ''; }
}
class FakeToast {
  success = jasmine.createSpy();
  warning = jasmine.createSpy();
  error = jasmine.createSpy();
}

function fakeRoute(params: Record<string, string>) {
  return {
    snapshot: { queryParamMap: convertToParamMap(params) }
  };
}

describe('ReachabilityComponent - Iteration 81 + 88', () => {
  function konfiguriere(params: Record<string, string>): void {
    TestBed.configureTestingModule({
      imports: [ReachabilityComponent],
      providers: [
        provideRouter([]),
        { provide: ReachabilityQueryService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useClass: FakeToast },
        { provide: ActivatedRoute, useValue: fakeRoute(params) }
      ]
    });
  }

  it('Iteration 81: hat einen "Zur Fix-Verifikation"-Header-Button', () => {
    konfiguriere({});
    const fixture = TestBed.createComponent(ReachabilityComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="reachability-to-fix-verification"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/fix-verification');
  });

  it('Iteration 88: queryParam findingId vorbelegt das Startdialog-Feld', () => {
    konfiguriere({ findingId: 'abc-123' });
    const fixture = TestBed.createComponent(ReachabilityComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.neueFindingId()).toBe('abc-123');
  });

  it('Iteration 88: Zeilen-Klick oeffnet Detail-Panel', async () => {
    konfiguriere({});
    const fixture = TestBed.createComponent(ReachabilityComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector(
      '[data-testid="reach-row-r1"]'
    ) as HTMLElement;
    expect(row).not.toBeNull();
    row.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toEqual(ROW);
    expect(fixture.nativeElement.querySelector(
      '[data-testid="reachability-detail-panel"]'
    )).not.toBeNull();
  });
});
