import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { echartsRouteProviders } from '../../shared/charts/echarts-providers';
import {
  Severity,
  SeverityBadgeComponent
} from '../../shared/components/severity-badge.component';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import { LocaleService } from '../../core/i18n/locale.service';
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
export class DashboardComponent {
  private readonly locale = inject(LocaleService);
  private readonly chartTheme = inject(ChartThemeService);
  private readonly auth = inject(AuthService);

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
