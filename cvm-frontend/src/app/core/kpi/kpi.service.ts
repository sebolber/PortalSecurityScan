import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type Severity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'NOT_APPLICABLE';

export interface SlaBucket {
  readonly inSla: number;
  readonly gesamt: number;
  readonly quote: number;
}

export interface BurnDownPoint {
  readonly day: string;
  readonly open: number;
}

export interface KpiResult {
  readonly openBySeverity: Partial<Record<Severity, number>>;
  readonly burnDown: readonly BurnDownPoint[];
  readonly mttrDaysBySeverity: Partial<Record<Severity, number>>;
  readonly slaBySeverity: Partial<Record<Severity, SlaBucket>>;
  readonly automationRate: number;
  readonly calculatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly api = inject(ApiClient);

  compute(window: string = '90d'): Promise<KpiResult> {
    return firstValueFrom(
      this.api.get<KpiResult>(
        `/api/v1/kpis?window=${encodeURIComponent(window)}`
      )
    );
  }
}
