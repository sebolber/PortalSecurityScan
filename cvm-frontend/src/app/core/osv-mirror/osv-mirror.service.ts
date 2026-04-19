import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface OsvMirrorReloadResponse {
  readonly reloaded: boolean;
  readonly indexSize: number;
}

/**
 * HTTP-Wrapper um den Admin-Endpunkt
 * {@code POST /api/v1/admin/osv-mirror/reload} (Iteration 73,
 * CVM-310). Der Aufruf erfordert Rolle {@code CVM_ADMIN}; das
 * Backend antwortet HTTP 503 {@code osv_mirror_inactive}, wenn
 * der Mirror nicht aktiviert ist.
 */
@Injectable({ providedIn: 'root' })
export class OsvMirrorService {
  private readonly api = inject(ApiClient);

  reload(): Promise<OsvMirrorReloadResponse> {
    return firstValueFrom(
      this.api.post<OsvMirrorReloadResponse, Record<string, never>>(
        '/api/v1/admin/osv-mirror/reload',
        {}
      )
    );
  }
}
