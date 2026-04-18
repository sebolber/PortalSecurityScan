import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type CveSeverity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL';

export interface CveView {
  readonly id: string;
  readonly cveId: string;
  readonly summary: string | null;
  readonly cvssBaseScore: number | null;
  readonly cvssVector: string | null;
  readonly kevListed: boolean;
  readonly epssScore: number | null;
  readonly epssPercentile: number | null;
  readonly cwes: readonly string[];
  readonly publishedAt: string | null;
  readonly lastModifiedAt: string | null;
  readonly source: string;
}

export interface CvePageResponse {
  readonly items: readonly CveView[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface CveQueryParams {
  readonly q?: string | null;
  readonly severity?: CveSeverity | null;
  readonly kev?: boolean;
  readonly page?: number;
  readonly size?: number;
}

@Injectable({ providedIn: 'root' })
export class CvesService {
  private readonly api = inject(ApiClient);

  findPage(params: CveQueryParams = {}): Promise<CvePageResponse> {
    const query = new URLSearchParams();
    if (params.q?.trim()) {
      query.set('q', params.q.trim());
    }
    if (params.severity) {
      query.set('severity', params.severity);
    }
    query.set('kev', String(params.kev ?? false));
    query.set('page', String(params.page ?? 0));
    query.set('size', String(params.size ?? 20));
    return firstValueFrom(
      this.api.get<CvePageResponse>(`/api/v1/cves?${query.toString()}`)
    );
  }
}
