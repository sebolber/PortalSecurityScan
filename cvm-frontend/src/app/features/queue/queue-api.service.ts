import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiClient } from '../../core/api/api-client.service';
import {
  ApproveCommand,
  QueueEntry,
  QueueFilter,
  RejectCommand
} from './queue.types';

/**
 * HTTP-Adapter auf die CVM-Queue-Endpunkte.
 *
 * <p>Gruende den HTTP-Zugriff an einer Stelle buendeln:
 * <ul>
 *   <li>URL-Aufbau konsistent halten.</li>
 *   <li>Server-seitige Filter ({@code productVersionId}, {@code
 *       environmentId}, {@code source}, {@code status}) werden als Query
 *       angehaengt. Severity-Filter laufen zurzeit client-seitig.</li>
 * </ul>
 */
@Injectable({ providedIn: 'root' })
export class QueueApiService {
  private readonly api = inject(ApiClient);

  list(filter: QueueFilter): Observable<QueueEntry[]> {
    const params = this.buildParams(filter);
    const query = params.toString();
    const path = query ? `/api/v1/findings?${query}` : '/api/v1/findings';
    return this.api.get<QueueEntry[]>(path);
  }

  approve(assessmentId: string, command: ApproveCommand): Observable<QueueEntry> {
    return this.api.post<QueueEntry, ApproveCommand>(
      `/api/v1/assessments/${assessmentId}/approve`,
      command
    );
  }

  reject(assessmentId: string, command: RejectCommand): Observable<QueueEntry> {
    return this.api.post<QueueEntry, RejectCommand>(
      `/api/v1/assessments/${assessmentId}/reject`,
      command
    );
  }

  /**
   * Iteration 87 (CVM-327): Audit-Trail eines Findings.
   * `GET /api/v1/findings/{id}/assessments/history` liefert alle
   * Assessment-Versionen in chronologischer Reihenfolge.
   */
  history(findingId: string): Observable<QueueEntry[]> {
    return this.api.get<QueueEntry[]>(
      `/api/v1/findings/${findingId}/assessments/history`
    );
  }

  private buildParams(filter: QueueFilter): HttpParams {
    let params = new HttpParams();
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.productVersionId) {
      params = params.set('productVersionId', filter.productVersionId);
    }
    if (filter.environmentId) {
      params = params.set('environmentId', filter.environmentId);
    }
    if (filter.source) {
      params = params.set('source', filter.source);
    }
    return params;
  }
}
