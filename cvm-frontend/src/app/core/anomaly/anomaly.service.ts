import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface AnomalyView {
  readonly id: string;
  readonly assessmentId: string | null;
  readonly pattern: string;
  readonly severity: string;
  readonly reason: string;
  readonly triggeredAt: string;
}

export interface AnomalyCount {
  readonly count: number;
}

@Injectable({ providedIn: 'root' })
export class AnomalyService {
  private readonly api = inject(ApiClient);

  list(hours = 24): Promise<AnomalyView[]> {
    return firstValueFrom(
      this.api.get<AnomalyView[]>(`/api/v1/anomalies?hours=${hours}`)
    );
  }

  count24h(): Promise<AnomalyCount> {
    return firstValueFrom(this.api.get<AnomalyCount>('/api/v1/anomalies/count'));
  }
}
