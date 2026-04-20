import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type AhsSeverityKey =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'NOT_APPLICABLE';

export interface DashboardKpiView {
  readonly offeneCves: number;
  readonly severityVerteilung: Readonly<Record<AhsSeverityKey, number>>;
  readonly aeltesteKritisch: {
    readonly cveKey: string;
    readonly tage: number;
  } | null;
  readonly weiterbetriebOk: boolean;
}

/**
 * Iteration 100 (CVM-342): liest die Dashboard-KPIs, die frueher
 * im Frontend hart-codiert waren.
 */
@Injectable({ providedIn: 'root' })
export class DashboardKpiService {
  private readonly api = inject(ApiClient);

  load(): Promise<DashboardKpiView> {
    return firstValueFrom(this.api.get<DashboardKpiView>('/api/v1/dashboard/kpi'));
  }
}
