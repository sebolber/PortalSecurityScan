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

  /** Iteration 85 (CVM-325). POST /api/v1/waivers/{id}/extend */
  extend(
    id: string,
    validUntil: string,
    extendedBy: string
  ): Promise<WaiverView> {
    return firstValueFrom(
      this.api.post<WaiverView, { validUntil: string; extendedBy: string }>(
        `/api/v1/waivers/${id}/extend`,
        { validUntil, extendedBy }
      )
    );
  }

  /** Iteration 85 (CVM-325). POST /api/v1/waivers/{id}/revoke */
  revoke(
    id: string,
    revokedBy: string,
    reason: string
  ): Promise<WaiverView> {
    return firstValueFrom(
      this.api.post<WaiverView, { revokedBy: string; reason: string }>(
        `/api/v1/waivers/${id}/revoke`,
        { revokedBy, reason }
      )
    );
  }
}
