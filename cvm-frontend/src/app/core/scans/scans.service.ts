import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from '../config/app-config.service';
import { ApiClient } from '../api/api-client.service';

export interface ScanUploadResponse {
  readonly scanId: string;
  readonly statusUri: string;
}

export interface ScanSummary {
  readonly scanId: string;
  readonly productVersionId: string;
  readonly environmentId: string | null;
  readonly scanner: string;
  readonly contentSha256: string;
  readonly scannedAt: string;
  readonly componentCount: number;
  readonly findingCount: number;
}

export interface ScanUploadParams {
  readonly productVersionId: string;
  readonly environmentId?: string | null;
  readonly scanner?: string;
}

/**
 * Upload einer CycloneDX-SBOM via {@code POST /api/v1/scans} (multipart)
 * sowie Status-Abfrage via {@code GET /api/v1/scans/{id}}.
 *
 * <p>Multipart wird direkt gegen den {@link HttpClient} gesendet, da der
 * {@link ApiClient} auf JSON ausgelegt ist. Die Base-URL kommt aus dem
 * {@link AppConfigService}, damit Dev- und Prod-Deploy gleich funktionieren.
 */
@Injectable({ providedIn: 'root' })
export class ScansService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiClient);
  private readonly config = inject(AppConfigService);

  async uploadSbom(params: ScanUploadParams, file: File): Promise<ScanUploadResponse> {
    const base = this.config.get().apiBaseUrl.replace(/\/$/, '');
    const query = new URLSearchParams();
    query.set('productVersionId', params.productVersionId);
    if (params.environmentId) {
      query.set('environmentId', params.environmentId);
    }
    query.set('scanner', params.scanner ?? 'trivy');
    const url = `${base}/api/v1/scans?${query.toString()}`;
    const form = new FormData();
    form.append('sbom', file);
    return firstValueFrom(
      this.http.post<ScanUploadResponse>(url, form)
    );
  }

  getStatus(scanId: string): Promise<ScanSummary> {
    return firstValueFrom(
      this.api.get<ScanSummary>(`/api/v1/scans/${scanId}`)
    );
  }
}
