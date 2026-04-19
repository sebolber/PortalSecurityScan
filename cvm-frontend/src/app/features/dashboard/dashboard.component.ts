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

  readonly offene = 0;

  readonly severityVerteilung: readonly SeverityCount[] = [
    { severity: 'CRITICAL', anzahl: 0 },
    { severity: 'HIGH', anzahl: 0 },
    { severity: 'MEDIUM', anzahl: 0 },
    { severity: 'LOW', anzahl: 0 }
  ];

  readonly aeltesteCritical = '–';
  readonly weiterbetriebOk = true;

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
    if (this.darfReports()) {
      await this.ladeLetzteReports();
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
          data: this.severityVerteilung.map((e) => ({
            name: e.severity,
            value: e.anzahl,
            itemStyle: { color: colors[e.severity] }
          }))
        }
      ]
    };
  });
}
