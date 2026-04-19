import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../core/auth/auth.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';
import { LocaleService } from '../../core/i18n/locale.service';

class FakeAuth {
  private roles: string[] = [];
  loggedIn = () => true;
  userRoles = () => this.roles;
  username = () => 't.tester@ahs.test';
  hasRole = (role: string) => this.roles.includes(role);
  setRoles(r: string[]): void { this.roles = r; }
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> { return ''; }
}

describe('DashboardComponent - Iteration 80 Handlungskarten', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useClass: FakeAuth },
        LocaleService,
        ChartThemeService
      ]
    });
  });

  it('ADMIN sieht alle drei Aktionskarten', () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_ADMIN']);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-action-scan"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="dashboard-action-queue"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="dashboard-action-waivers"]'))
      .not.toBeNull();
  });

  it('VIEWER sieht keinen Scan-Link, aber Waiver und Queue entfallen bei fehlenden Rollen', () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_VIEWER']);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-action-scan"]'))
      .toBeNull();
    expect(el.querySelector('[data-testid="dashboard-action-queue"]'))
      .toBeNull();
    // Waiver ist fuer VIEWER sichtbar.
    expect(el.querySelector('[data-testid="dashboard-action-waivers"]'))
      .not.toBeNull();
  });
});
