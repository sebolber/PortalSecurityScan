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

/**
 * Request fuer POST /api/v1/findings/&#123;id&#125;/reachability.
 * Pflichtfelder aus ReachabilityController.ReachabilityApiRequest:
 * {@code repoUrl}, {@code vulnerableSymbol}, {@code triggeredBy}.
 */
export interface ReachabilityStartRequest {
  readonly repoUrl: string;
  readonly branch?: string | null;
  readonly commitSha?: string | null;
  readonly vulnerableSymbol: string;
  readonly language?: string | null;
  readonly instruction?: string | null;
  readonly triggeredBy: string;
}

export interface ReachabilityCallSite {
  readonly file: string;
  readonly line: number | null;
  readonly snippet: string | null;
}

export interface ReachabilityResult {
  readonly recommendation: string | null;
  readonly summary: string | null;
  readonly callSites: readonly ReachabilityCallSite[];
}

export interface ReachabilitySuggestion {
  readonly findingId: string;
  readonly sourcePurl: string | null;
  readonly symbol: string | null;
  readonly language: string | null;
  readonly rationale: string | null;
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

  /**
   * Startet die Reachability-Analyse fuer ein Finding. Blockiert, bis
   * der Backend-Subprocess fertig ist (`cvm.ai.reachability.timeout-
   * seconds`-Deckel) - Aufrufer sollten einen Ladezustand anzeigen.
   */
  startAnalysis(
    findingId: string,
    request: ReachabilityStartRequest
  ): Promise<ReachabilityResult> {
    return firstValueFrom(
      this.api.post<ReachabilityResult, ReachabilityStartRequest>(
        `/api/v1/findings/${encodeURIComponent(findingId)}/reachability`,
        request
      )
    );
  }

  /**
   * Holt einen abgeleiteten Symbol-Vorschlag (aus der Component-PURL
   * des Findings). {@code symbol} kann null sein, wenn die PURL nicht
   * parsebar ist - Aufrufer muss dann auf manuelle Eingabe fallback.
   */
  suggestion(findingId: string): Promise<ReachabilitySuggestion> {
    return firstValueFrom(
      this.api.get<ReachabilitySuggestion>(
        `/api/v1/findings/${encodeURIComponent(findingId)}/reachability/suggestion`
      )
    );
  }
}
