import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface AlertHistoryView {
  readonly id: string;
  readonly ruleId: string;
  readonly triggerKey: string;
  readonly dispatchedAt: string;
  readonly recipients: readonly string[];
  readonly subject: string;
  readonly bodyExcerpt: string | null;
  readonly dryRun: boolean;
  readonly error: string | null;
}

@Injectable({ providedIn: 'root' })
export class AlertsHistoryService {
  private readonly api = inject(ApiClient);

  recent(limit = 50): Promise<AlertHistoryView[]> {
    return firstValueFrom(
      this.api.get<AlertHistoryView[]>(`/api/v1/alerts/history?limit=${limit}`)
    );
  }
}
