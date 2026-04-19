import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../api/api-client.service';
import { AppConfigService } from '../config/app-config.service';

export type AhsSeverity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'NOT_APPLICABLE';

export interface ReportRequest {
  readonly productVersionId: string;
  readonly environmentId: string;
  readonly gesamteinstufung: AhsSeverity;
  readonly freigeberKommentar?: string;
  readonly erzeugtVon: string;
  readonly stichtag?: string;
}

export interface ReportResponse {
  readonly reportId: string;
  readonly productVersionId: string;
  readonly environmentId: string;
  readonly reportType: string;
  readonly title: string;
  readonly gesamteinstufung: AhsSeverity;
  readonly erzeugtVon: string;
  readonly erzeugtAm: string;
  readonly stichtag: string;
  readonly sha256: string;
}

export interface ReportListResponse {
  readonly items: readonly ReportResponse[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface ReportListQuery {
  readonly productVersionId?: string;
  readonly environmentId?: string;
  readonly page?: number;
  readonly size?: number;
}

/**
 * Frontend-Anbindung an die PDF-Report-API (Iteration 10):
 * <ul>
 *   <li>{@code POST /api/v1/reports/hardening} erzeugt einen Report,
 *       liefert Metadaten zurueck.</li>
 *   <li>{@code GET /api/v1/reports/{id}} streamt das PDF; wir
 *       formen daraus eine Blob-URL fuer das Browser-Download.</li>
 * </ul>
 *
 * Liste der Reports wird im Frontend gehalten - das Backend hat
 * (noch) keinen Listing-Endpoint, das ist offener Punkt.
 */
@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  erzeuge(request: ReportRequest): Observable<ReportResponse> {
    return this.api.post<ReportResponse, ReportRequest>(
      '/api/v1/reports/hardening',
      request
    );
  }

  ladePdf(reportId: string): Observable<Blob> {
    const url = this.config.get().apiBaseUrl.replace(/\/$/, '')
      + '/api/v1/reports/' + reportId;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Iteration 93 (CVM-333): Report-Historie. Mappt die optionalen
   * Filter auf queryParams und liefert eine paginierte Liste.
   */
  list(query: ReportListQuery = {}): Observable<ReportListResponse> {
    const qp: Record<string, string> = {};
    if (query.productVersionId) qp['productVersionId'] = query.productVersionId;
    if (query.environmentId) qp['environmentId'] = query.environmentId;
    if (query.page !== undefined) qp['page'] = String(query.page);
    if (query.size !== undefined) qp['size'] = String(query.size);
    const search = new URLSearchParams(qp).toString();
    const suffix = search.length > 0 ? '?' + search : '';
    return this.api.get<ReportListResponse>('/api/v1/reports' + suffix);
  }
}
