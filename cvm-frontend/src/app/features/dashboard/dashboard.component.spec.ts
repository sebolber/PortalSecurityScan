import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { AlertBannerService } from '../../core/alerts/alert-banner.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  DashboardKpiService,
  DashboardKpiView
} from '../../core/dashboard/dashboard-kpi.service';
import {
  ReportListResponse,
  ReportsService
} from '../../core/reports/reports.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';
import { LocaleService } from '../../core/i18n/locale.service';
import { of } from 'rxjs';
import { signal } from '@angular/core';

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

class FakeAlertBanner {
  readonly statusSig = signal<
    { visible: boolean; count: number; t2Minutes: number } | null
  >(null);
  readonly status = this.statusSig.asReadonly();
  async refresh(): Promise<void> {}
}

class FakeReports {
  listResponse: ReportListResponse = {
    items: [],
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0
  };
  list = jasmine.createSpy('list').and.callFake(() => of(this.listResponse));
}

class FakeKpi {
  response: DashboardKpiView = {
    offeneCves: 0,
    severityVerteilung: {
      CRITICAL: 0,
      HIGH: 0,
      MEDIUM: 0,
      LOW: 0,
      INFORMATIONAL: 0,
      NOT_APPLICABLE: 0
    },
    aeltesteKritisch: null,
    weiterbetriebOk: true
  };
  load = jasmine.createSpy('load').and.callFake(() => Promise.resolve(this.response));
}

function configure(): void {
  TestBed.configureTestingModule({
    imports: [DashboardComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useClass: FakeAuth },
      { provide: AlertBannerService, useClass: FakeAlertBanner },
      { provide: ReportsService, useClass: FakeReports },
      { provide: DashboardKpiService, useClass: FakeKpi },
      LocaleService,
      ChartThemeService
    ]
  });
}

describe('DashboardComponent - Iteration 80 Handlungskarten', () => {
  beforeEach(() => configure());

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

describe('DashboardComponent - Iteration 94 Handlungszentrale', () => {
  beforeEach(() => configure());

  it('ADMIN laedt beim Init die letzten Reports', async () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_ADMIN']);
    const reports = TestBed.inject(ReportsService) as unknown as FakeReports;
    reports.listResponse = {
      items: [
        {
          reportId: 'r1',
          productVersionId: 'pv',
          environmentId: 'e',
          reportType: 'HARDENING',
          title: 'Alpha',
          gesamteinstufung: 'HIGH',
          erzeugtVon: 't',
          erzeugtAm: '2026-04-19T00:00:00Z',
          stichtag: '2026-04-19T00:00:00Z',
          sha256: 'abc'
        }
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1
    };
    const fixture = TestBed.createComponent(DashboardComponent);
    await fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    expect(reports.list).toHaveBeenCalledWith({ size: 5 });
    expect(fixture.componentInstance.letzteReports().length).toBe(1);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-letzte-reports"]'))
      .not.toBeNull();
  });

  it('Nutzer ohne Report-Rollen sieht die Reports-Card nicht', async () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_ASSESSOR']);
    const reports = TestBed.inject(ReportsService) as unknown as FakeReports;
    const fixture = TestBed.createComponent(DashboardComponent);
    await fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    expect(reports.list).not.toHaveBeenCalled();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-letzte-reports"]'))
      .toBeNull();
  });

  it('Rendert T2-Ampel CRITICAL, wenn AlertBanner sichtbar ist', () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_VIEWER']);
    const banner = TestBed.inject(AlertBannerService) as unknown as FakeAlertBanner;
    banner.statusSig.set({ visible: true, count: 7, t2Minutes: 60 });
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-t2-critical"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="dashboard-t2-ok"]'))
      .toBeNull();
  });

  it('Rendert T2-Ampel LOW, wenn AlertBanner nicht sichtbar ist', () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_VIEWER']);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-t2-ok"]'))
      .not.toBeNull();
  });

  it('Report-Laden-Fehler wird im UI sichtbar', async () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_ADMIN']);
    const reports = TestBed.inject(ReportsService) as unknown as FakeReports;
    reports.list.and.callFake(() => {
      throw new Error('boom');
    });
    const fixture = TestBed.createComponent(DashboardComponent);
    await fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    expect(fixture.componentInstance.letzteReportsFehler()).toBeTrue();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="dashboard-reports-error"]'))
      .not.toBeNull();
  });
});

describe('DashboardComponent - Iteration 100 KPI-Endpoint', () => {
  beforeEach(() => configure());

  it('laedt die KPIs beim Init und rendert offeneCves', async () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_VIEWER']);
    const kpi = TestBed.inject(DashboardKpiService) as unknown as FakeKpi;
    kpi.response = {
      offeneCves: 42,
      severityVerteilung: {
        CRITICAL: 7,
        HIGH: 12,
        MEDIUM: 18,
        LOW: 5,
        INFORMATIONAL: 0,
        NOT_APPLICABLE: 0
      },
      aeltesteKritisch: { cveKey: 'CVE-2017-18640', tage: 33 },
      weiterbetriebOk: false
    };

    const fixture = TestBed.createComponent(DashboardComponent);
    await fixture.componentInstance.ngOnInit();
    fixture.detectChanges();

    expect(kpi.load).toHaveBeenCalled();
    expect(fixture.componentInstance.offene()).toBe(42);
    expect(fixture.componentInstance.weiterbetriebOk()).toBeFalse();
    expect(fixture.componentInstance.aeltesteCritical())
      .toContain('CVE-2017-18640');
    expect(fixture.componentInstance.aeltesteCritical()).toContain('33');
  });

  it('KPI-Laden-Fehler setzt das Fehler-Signal und fallbacked auf 0', async () => {
    const auth = TestBed.inject(AuthService) as unknown as FakeAuth;
    auth.setRoles(['CVM_VIEWER']);
    const kpi = TestBed.inject(DashboardKpiService) as unknown as FakeKpi;
    kpi.load.and.callFake(() => Promise.reject(new Error('boom')));

    const fixture = TestBed.createComponent(DashboardComponent);
    await fixture.componentInstance.ngOnInit();

    expect(fixture.componentInstance.kpiFehler()).toBeTrue();
    expect(fixture.componentInstance.offene()).toBe(0);
    expect(fixture.componentInstance.weiterbetriebOk()).toBeTrue();
  });

  it('aeltesteCritical rendert "-", wenn kein CRITICAL offen ist', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.aeltesteCritical()).toBe('-');
  });
});
