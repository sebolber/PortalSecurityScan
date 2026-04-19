import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { echartsRouteProviders } from '../../shared/charts/echarts-providers';
import {
  KpiResult,
  KpiService,
  Severity
} from '../../core/kpi/kpi.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

const FENSTER: readonly { key: string; label: string }[] = [
  { key: '30d', label: '30 Tage' },
  { key: '90d', label: '90 Tage' },
  { key: '180d', label: '180 Tage' }
];

const SEVERITIES: readonly Severity[] = [
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
  'INFORMATIONAL'
];

/**
 * Mandanten-KPI-Dashboard (Iteration 27d, CVM-64). Ersetzt den
 * Platzhalter aus 27b durch die Anbindung an
 * {@code GET /api/v1/kpis}: offene Findings je Severity,
 * MTTR-Tage, Fix-SLA-Quote, Automatisierungsquote und
 * Burn-Down-Verlauf.
 */
@Component({
  selector: 'cvm-tenant-kpi',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    DecimalPipe,
    PercentPipe,
    NgxEchartsDirective,
    AhsBannerComponent,
    CvmIconComponent
  ],
  // Iteration 52 (CVM-102): ECharts lazy in den Tenant-KPI-Chunk.
  providers: [echartsRouteProviders()],
  templateUrl: './tenant-kpi.component.html',
  styleUrls: ['./tenant-kpi.component.scss']
})
export class TenantKpiComponent implements OnInit {
  private readonly kpi = inject(KpiService);
  private readonly chartTheme = inject(ChartThemeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly fenster = FENSTER;
  readonly severities = SEVERITIES;

  window = signal<string>('90d');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<KpiResult | null>(null);

  /**
   * Iteration 55 (CVM-105): Severity-Saeulen-Diagramm. Zeigt die
   * offenen Findings pro Severity als Balken mit Severity-Farbe.
   */
  readonly severityBarOption = computed<EChartsOption>(() => {
    const colors = this.chartTheme.severityColors();
    const sevs = this.severities;
    const values = sevs.map((s) => this.openCount(s));
    return {
      tooltip: { trigger: 'item' },
      grid: { left: 40, right: 20, top: 20, bottom: 40 },
      xAxis: {
        type: 'category',
        data: sevs as string[],
        axisLabel: { color: this.chartTheme.textColor() }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: this.chartTheme.textColor() }
      },
      series: [
        {
          type: 'bar',
          data: sevs.map((s, i) => ({
            value: values[i],
            itemStyle: { color: colors[s] }
          }))
        }
      ]
    };
  });

  readonly burnDownOption = computed<EChartsOption>(() => {
    const data = this.result()?.burnDown ?? [];
    const color = this.chartTheme.severityColors()['CRITICAL'];
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: data.map((p) => p.day),
        axisLabel: { color: this.chartTheme.textColor() }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: this.chartTheme.textColor() }
      },
      series: [
        {
          type: 'line',
          smooth: true,
          data: data.map((p) => p.open),
          itemStyle: { color },
          areaStyle: { color, opacity: 0.15 }
        }
      ]
    };
  });

  ngOnInit(): void {
    // Iteration 83 (CVM-323): Zeitfenster aus queryParams.
    const qp = this.route.snapshot.queryParamMap.get('window');
    if (qp && FENSTER.some((f) => f.key === qp)) {
      this.window.set(qp);
    }
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.result.set(await this.kpi.compute(this.window()));
    } catch {
      this.error.set('KPIs konnten nicht geladen werden.');
      this.result.set(null);
    } finally {
      this.loading.set(false);
    }
  }

  fensterWechseln(value: string): void {
    this.window.set(value);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { window: value === '90d' ? null : value },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
    void this.laden();
  }

  openCount(severity: Severity): number {
    return this.result()?.openBySeverity?.[severity] ?? 0;
  }

  mttr(severity: Severity): number {
    return this.result()?.mttrDaysBySeverity?.[severity] ?? 0;
  }

  slaQuote(severity: Severity): number {
    return this.result()?.slaBySeverity?.[severity]?.quote ?? 1;
  }

  /**
   * Iteration 55 (CVM-105): SLA-Ampel pro Severity.
   * - gruen: SLA-Quote &ge; 0.95
   * - gelb:  0.80 &le; Quote &lt; 0.95
   * - rot:   Quote &lt; 0.80
   */
  slaAmpel(severity: Severity): 'green' | 'yellow' | 'red' {
    const q = this.slaQuote(severity);
    if (q >= 0.95) {
      return 'green';
    }
    if (q >= 0.8) {
      return 'yellow';
    }
    return 'red';
  }

  ampelColor(severity: Severity): string {
    const a = this.slaAmpel(severity);
    return a === 'green' ? '#16a34a' : a === 'yellow' ? '#f59e0b' : '#dc2626';
  }
}
