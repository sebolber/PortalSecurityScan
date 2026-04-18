import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type VerificationGrade = 'A' | 'B' | 'C' | 'UNKNOWN';
export type MitigationStatus =
  | 'OPEN'
  | 'PLANNED'
  | 'IN_PROGRESS'
  | 'IMPLEMENTED'
  | 'VERIFIED'
  | 'WAIVED';
export type MitigationStrategy =
  | 'UPGRADE'
  | 'PATCH'
  | 'CONFIG_CHANGE'
  | 'WORKAROUND'
  | 'ACCEPT_RISK'
  | string;

export interface FixVerificationSummaryView {
  readonly id: string;
  readonly assessmentId: string | null;
  readonly status: MitigationStatus;
  readonly strategy: MitigationStrategy;
  readonly targetVersion: string | null;
  readonly owner: string | null;
  readonly plannedFor: string | null;
  readonly implementedAt: string | null;
  readonly verificationGrade: VerificationGrade | null;
  readonly verificationEvidenceType: string | null;
  readonly verifiedAt: string | null;
  readonly createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FixVerificationQueryHttpService {
  private readonly api = inject(ApiClient);

  list(
    grade: VerificationGrade | null = null,
    limit = 50
  ): Promise<FixVerificationSummaryView[]> {
    const params = [`limit=${limit}`];
    if (grade) {
      params.push(`grade=${encodeURIComponent(grade)}`);
    }
    return firstValueFrom(
      this.api.get<FixVerificationSummaryView[]>(
        `/api/v1/fix-verification?${params.join('&')}`
      )
    );
  }
}
