import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { AhsCardComponent } from '../../shared/components/ahs-card.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { LocaleService } from '../../core/i18n/locale.service';

interface SeverityCount {
  readonly severity: string;
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
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  private readonly locale = inject(LocaleService);

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

  readonly chartOption: EChartsOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: this.severityVerteilung.map((e) => ({
          name: e.severity,
          value: e.anzahl
        }))
      }
    ]
  };
}
