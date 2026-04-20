import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { echartsRouteProviders } from '../../shared/charts/echarts-providers';
import {
  Severity,
  SeverityBadgeComponent
} from '../../shared/components/severity-badge.component';
import { AlertBannerService } from '../../core/alerts/alert-banner.service';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import {
  DashboardKpiService,
  DashboardKpiView
} from '../../core/dashboard/dashboard-kpi.service';
import { LocaleService } from '../../core/i18n/locale.service';
import {
  ReportResponse,
  ReportsService
} from '../../core/reports/reports.service';
import { OnboardingService } from '../onboarding/onboarding.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

interface SeverityCount {
  readonly severity: Severity;
  readonly anzahl: number;
}

/**
 * Dashboard-Geruest fuer Iteration 07. Zeigt vier Cards:
 * <ol>
 *   <li>Kennzahl: offene CVEs gesamt.</li>
 *   <li>Severity-Donut (ECharts).</li>
 *   <li>Aelteste offene CRITICAL-CVE.</li>
 *   <li>Ampel "Weiterbetrieb moeglich?".</li>
 * </ol>
 *
 * Iteration 61 (CVM-62): Material entfernt, reines Tailwind via
 * `.card`, `.card-header`, `.card-body`, `.page`, `.page-title`.
 */
@Component({
  selector: 'cvm-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    SeverityBadgeComponent,
    NgxEchartsDirective,
    CvmIconComponent
  ],
  // Iteration 52 (CVM-102): ECharts lazy in den Dashboard-Chunk.
  providers: [echartsRouteProviders()],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private readonly locale = inject(LocaleService);
  private readonly chartTheme = inject(ChartThemeService);
  private readonly auth = inject(AuthService);
  private readonly reports = inject(ReportsService);
  private readonly alerts = inject(AlertBannerService);
  private readonly onboarding = inject(OnboardingService);
  private readonly kpiService = inject(DashboardKpiService);

  readonly texte = this.locale.messages.dashboard;

  // Iteration 80 (CVM-320): Rollen-gefilterte Handlungskarten
  // oberhalb der KPIs. Jede Karte ist ein Deep-Link in den
  // naechsten Workflow-Schritt.
  readonly darfScan = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.ADMIN) ||
      this.auth.hasRole(CVM_ROLES.ASSESSOR)
  );
  readonly darfQueue = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.ADMIN) ||
      this.auth.hasRole(CVM_ROLES.ASSESSOR) ||
      this.auth.hasRole(CVM_ROLES.REVIEWER) ||
      this.auth.hasRole(CVM_ROLES.APPROVER)
  );
  readonly darfWaiver = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.ADMIN) ||
      this.auth.hasRole(CVM_ROLES.VIEWER) ||
      this.auth.hasRole(CVM_ROLES.ASSESSOR) ||
      this.auth.hasRole(CVM_ROLES.REVIEWER) ||
      this.auth.hasRole(CVM_ROLES.APPROVER)
  );

  // Iteration 100 (CVM-342): echte KPIs aus dem Backend.
  readonly kpi = signal<DashboardKpiView | null>(null);
  readonly kpiFehler = signal<boolean>(false);

  readonly offene = computed<number>(() => this.kpi()?.offeneCves ?? 0);

  readonly severityVerteilung = computed<readonly SeverityCount[]>(() => {
    const v = this.kpi()?.severityVerteilung;
    const sev: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    return sev.map((s) => ({ severity: s, anzahl: v ? (v[s] ?? 0) : 0 }));
  });

  readonly aeltesteCritical = computed<string>(() => {
    const k = this.kpi()?.aeltesteKritisch;
    if (!k) return '-';
    const tageWort = k.tage === 1 ? 'Tag' : 'Tage';
    return `${k.cveKey} (${k.tage} ${tageWort})`;
  });

  readonly weiterbetriebOk = computed<boolean>(
    () => this.kpi()?.weiterbetriebOk ?? true
  );

  // Iteration 94 (CVM-334): Dashboard als Handlungszentrale -
  // letzte 5 Reports und aktuelle T2-Eskalations-Anzahl.
  readonly letzteReports = signal<readonly ReportResponse[]>([]);
  readonly letzteReportsLaedt = signal<boolean>(false);
  readonly letzteReportsFehler = signal<boolean>(false);
  readonly alertStatus = this.alerts.status;
  readonly darfReports = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.ADMIN) ||
      this.auth.hasRole(CVM_ROLES.REPORTER) ||
      this.auth.hasRole(CVM_ROLES.VIEWER)
  );

  // Iteration 96 (CVM-336): Onboarding-CTA, solange Admin nicht fertig.
  readonly onboardingState = this.onboarding.state;
  readonly zeigeOnboardingCta = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.ADMIN) &&
      !this.onboarding.completed()
  );

  async ngOnInit(): Promise<void> {
    // Dashboard ist eine public Route (app.routes.ts), damit die Shell
    // auch ohne Keycloak-Session sichtbar ist. Die KPI- und Report-
    // Endpoints verlangen jedoch einen Bearer-Token; ohne Login laeuft
    // jeder Aufruf in ein 401 und wuerde den User mit einem
    // missverstaendlichen Fehler-Toast begruessen. Wir laden die Daten
    // daher erst, sobald der User angemeldet ist.
    if (!this.auth.loggedIn()) {
      return;
    }
    await this.ladeKpi();
    if (this.darfReports()) {
      await this.ladeLetzteReports();
    }
  }

  async ladeKpi(): Promise<void> {
    this.kpiFehler.set(false);
    try {
      this.kpi.set(await this.kpiService.load());
    } catch {
      this.kpi.set(null);
      this.kpiFehler.set(true);
    }
  }

  async ladeLetzteReports(): Promise<void> {
    this.letzteReportsLaedt.set(true);
    this.letzteReportsFehler.set(false);
    try {
      const res = await firstValueFrom(this.reports.list({ size: 5 }));
      this.letzteReports.set(res.items);
    } catch {
      this.letzteReports.set([]);
      this.letzteReportsFehler.set(true);
    } finally {
      this.letzteReportsLaedt.set(false);
    }
  }

  readonly chartOption = computed<EChartsOption>(() => {
    const colors = this.chartTheme.severityColors();
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: this.chartTheme.textColor() } },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 4,
            borderColor: this.chartTheme.sliceBorderColor(),
            borderWidth: 2
          },
          label: { show: false },
          data: this.severityVerteilung().map((e) => ({
            name: e.severity,
            value: e.anzahl,
            itemStyle: { color: colors[e.severity] }
          }))
        }
      ]
    };
  });
}
