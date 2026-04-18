import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type AiCallStatus =
  | 'PENDING'
  | 'OK'
  | 'INVALID_OUTPUT'
  | 'INJECTION_RISK'
  | 'ERROR'
  | 'RATE_LIMITED'
  | 'DISABLED';

export interface AiCallAuditView {
  readonly id: string;
  readonly useCase: string;
  readonly modelId: string;
  readonly status: AiCallStatus;
  readonly injectionRisk: boolean;
  readonly promptTokens: number | null;
  readonly completionTokens: number | null;
  readonly latencyMs: number | null;
  readonly costEur: number | null;
  readonly triggeredBy: string;
  readonly environmentId: string | null;
  readonly createdAt: string;
  readonly finalizedAt: string | null;
  readonly invalidOutputReason: string | null;
  readonly errorMessage: string | null;
}

export interface AiAuditPage {
  readonly content: AiCallAuditView[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface AiAuditFilter {
  readonly status?: AiCallStatus;
  readonly useCase?: string;
  readonly page?: number;
  readonly size?: number;
}

/**
 * Frontend-Anbindung an {@code GET /api/v1/ai/audits}
 * (Iteration 11 Nachzug). Nur lesend; das Backend erlaubt Zugriff
 * fuer {@code AI_AUDITOR} und {@code CVM_ADMIN}.
 */
@Injectable({ providedIn: 'root' })
export class AiAuditService {
  private readonly api = inject(ApiClient);

  liste(filter: AiAuditFilter = {}): Observable<AiAuditPage> {
    let params = new HttpParams();
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.useCase) {
      params = params.set('useCase', filter.useCase);
    }
    params = params.set('page', String(filter.page ?? 0));
    params = params.set('size', String(filter.size ?? 20));
    const query = params.toString();
    const path = '/api/v1/ai/audits' + (query ? '?' + query : '');
    return this.api.get<AiAuditPage>(path);
  }
}
