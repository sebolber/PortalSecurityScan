import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { QueueDetailComponent } from './queue-detail.component';
import { QueueEntry } from './queue.types';
import { AuthService } from '../../core/auth/auth.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { ReachabilityQueryService } from '../../core/reachability/reachability.service';

class FakeAuth {
  private name = 'ich@ahs.test';
  setName(n: string): void { this.name = n; }
  username = () => this.name;
  userRoles = () => ['CVM_APPROVER'];
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

class FakeReach {
  async start(): Promise<unknown> { return {}; }
  async suggestion(): Promise<unknown> { return null; }
}

function entry(decidedBy: string | null): QueueEntry {
  return {
    id: 'a1',
    findingId: 'f1',
    cveId: 'c1',
    cveKey: 'CVE-2025-48924',
    severity: 'HIGH',
    status: 'PROPOSED',
    source: 'AI',
    rationale: 'Vorschlag',
    decidedBy,
    version: 1,
    createdAt: '2026-01-01T00:00:00Z'
  };
}

describe('QueueDetailComponent - Iteration 86 Vier-Augen-Warnung', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QueueDetailComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useClass: FakeToast },
        { provide: ReachabilityQueryService, useClass: FakeReach }
      ]
    });
  });

  it('kein Konflikt: Banner nicht sichtbar, Approve-Button aktiv', () => {
    const fixture = TestBed.createComponent(QueueDetailComponent);
    fixture.componentInstance.entry = entry('andere@ahs.test');
    fixture.componentInstance.ngOnChanges();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(
      '[data-testid="queue-detail-selbstfreigabe-banner"]'
    )).toBeNull();
    const btn = fixture.nativeElement.querySelector(
      '[data-testid="queue-detail-approve"]'
    ) as HTMLButtonElement;
    expect(btn.disabled).toBeFalse();
  });

  it('Konflikt: Banner sichtbar, Approve-Button disabled', () => {
    const fixture = TestBed.createComponent(QueueDetailComponent);
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setName('ich@ahs.test');
    fixture.componentInstance.entry = entry('ich@ahs.test');
    fixture.componentInstance.ngOnChanges();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(
      '[data-testid="queue-detail-selbstfreigabe-banner"]'
    )).not.toBeNull();
    const btn = fixture.nativeElement.querySelector(
      '[data-testid="queue-detail-approve"]'
    ) as HTMLButtonElement;
    expect(btn.disabled).toBeTrue();
  });

  it('Entry-Wechsel aktualisiert den Konflikt-Status', () => {
    const fixture = TestBed.createComponent(QueueDetailComponent);
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setName('ich@ahs.test');
    fixture.componentInstance.entry = entry('ich@ahs.test');
    fixture.componentInstance.ngOnChanges();
    fixture.detectChanges();
    expect(fixture.componentInstance.selbstfreigabeKonflikt).toBeTrue();

    fixture.componentInstance.entry = entry('andere@ahs.test');
    fixture.componentInstance.ngOnChanges();
    fixture.detectChanges();
    expect(fixture.componentInstance.selbstfreigabeKonflikt).toBeFalse();
  });
});
