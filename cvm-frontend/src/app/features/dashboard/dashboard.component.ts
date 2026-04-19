import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { echartsRouteProviders } from '../../shared/charts/echarts-providers';
import { AhsCardComponent } from '../../shared/components/ahs-card.component';
import {
  Severity,
  SeverityBadgeComponent
} from '../../shared/components/severity-badge.component';
import { LocaleService } from '../../core/i18n/locale.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';

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
 * Daten sind statisch; Iteration 08 verbindet die Cards mit der
 * Bewertungs-Queue-API.
 */
@Component({
  selector: 'cvm-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    AhsCardComponent,
    SeverityBadgeComponent,
    NgxEchartsDirective
  ],
  // Iteration 52 (CVM-102): ECharts lazy in den Dashboard-Chunk.
  providers: [echartsRouteProviders()],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  private readonly locale = inject(LocaleService);
  private readonly chartTheme = inject(ChartThemeService);

  readonly texte = this.locale.messages.dashboard;

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
