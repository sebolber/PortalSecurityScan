import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WaiversComponent } from './waivers.component';
import {
  WaiverView,
  WaiversService
} from '../../core/waivers/waivers.service';
import { AuthService } from '../../core/auth/auth.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

const WAIVER: WaiverView = {
  id: 'w1',
  assessmentId: 'a1',
  reason: 'Risk accepted',
  grantedBy: 't.tester@ahs.test',
  validUntil: '2026-12-31T00:00:00Z',
  reviewIntervalDays: 90,
  status: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  extendedAt: null,
  revokedAt: null
};

class FakeApi {
  list = jasmine.createSpy('list').and.resolveTo([WAIVER]);
  extend = jasmine.createSpy('extend').and.resolveTo(WAIVER);
  revoke = jasmine.createSpy('revoke').and.resolveTo({ ...WAIVER, status: 'REVOKED' });
}

class FakeAuth {
  private name = 'a.admin@ahs.test';
  username = () => this.name;
  setName(n: string): void { this.name = n; }
  userRoles = () => ['CVM_ADMIN'];
  loggedIn = () => true;
  hasRole = () => true;
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

describe('WaiversComponent - Iteration 81 + 85', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WaiversComponent],
      providers: [
        provideRouter([]),
        { provide: WaiversService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useClass: FakeToast }
      ]
    });
  });

  it('Iteration 81: hat einen "Zum Hardening-Bericht"-Header-Button', () => {
    const fixture = TestBed.createComponent(WaiversComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="waivers-to-reports"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/reports');
  });

  it('Iteration 85: ACTIVE-Waiver zeigt Verlaengern- und Widerrufen-Buttons', async () => {
    const fixture = TestBed.createComponent(WaiversComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(
      `[data-testid="waiver-extend-${WAIVER.id}"]`
    )).not.toBeNull();
    expect(fixture.nativeElement.querySelector(
      `[data-testid="waiver-revoke-${WAIVER.id}"]`
    )).not.toBeNull();
  });

  it('Iteration 85: Verlaengern-Dialog mit Vier-Augen-Konflikt disabled den Confirm-Button', async () => {
    const fixture = TestBed.createComponent(WaiversComponent);
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setName('t.tester@ahs.test');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.oeffneExtendDialog(WAIVER);
    fixture.detectChanges();
    const confirm = fixture.nativeElement.querySelector(
      '[data-testid="waiver-extend-confirm"]'
    ) as HTMLButtonElement;
    expect(confirm).not.toBeNull();
    expect(confirm.disabled).toBeTrue();
  });

  it('Iteration 85: revoke ohne Begruendung => warning + kein API-Call', async () => {
    const fixture = TestBed.createComponent(WaiversComponent);
    const toast = TestBed.inject(CvmToastService) as unknown as FakeToast;
    const api = TestBed.inject(WaiversService) as unknown as FakeApi;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.oeffneRevokeDialog(WAIVER);
    fixture.detectChanges();
    await fixture.componentInstance.revokeBestaetigen();
    expect(toast.warning).toHaveBeenCalled();
    expect(api.revoke).not.toHaveBeenCalled();
  });
});
