import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type AiSuggestionStatus =
  | 'PROPOSED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SUPERSEDED';

export interface ReachabilitySummaryView {
  readonly id: string;
  readonly findingId: string | null;
  readonly status: AiSuggestionStatus;
  readonly severity: string | null;
  readonly rationale: string | null;
  readonly confidence: number | null;
  readonly createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ReachabilityQueryService {
  private readonly api = inject(ApiClient);

  list(limit = 50): Promise<ReachabilitySummaryView[]> {
    return firstValueFrom(
      this.api.get<ReachabilitySummaryView[]>(
        `/api/v1/reachability?limit=${limit}`
      )
    );
  }
}
