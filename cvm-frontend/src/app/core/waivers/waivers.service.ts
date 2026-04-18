import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type WaiverStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export interface WaiverView {
  readonly id: string;
  readonly assessmentId: string | null;
  readonly reason: string;
  readonly grantedBy: string;
  readonly validUntil: string;
  readonly reviewIntervalDays: number;
  readonly status: WaiverStatus;
  readonly createdAt: string;
  readonly extendedAt: string | null;
  readonly revokedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class WaiversService {
  private readonly api = inject(ApiClient);

  list(status: WaiverStatus = 'ACTIVE'): Promise<WaiverView[]> {
    const suffix = status ? `?status=${encodeURIComponent(status)}` : '';
    return firstValueFrom(this.api.get<WaiverView[]>(`/api/v1/waivers${suffix}`));
  }
}
