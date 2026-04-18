import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import {
  KpiResult,
  KpiService,
  Severity
} from '../../core/kpi/kpi.service';
import { ChartThemeService } from '../../core/theme/chart-theme.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';

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
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    NgxEchartsDirective,
    AhsBannerComponent
  ],
  templateUrl: './tenant-kpi.component.html',
  styleUrls: ['./tenant-kpi.component.scss']
})
export class TenantKpiComponent implements OnInit {
  private readonly kpi = inject(KpiService);
  private readonly chartTheme = inject(ChartThemeService);

  readonly fenster = FENSTER;
  readonly severities = SEVERITIES;

  window = signal<string>('90d');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<KpiResult | null>(null);

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
}
