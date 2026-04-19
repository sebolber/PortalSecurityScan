import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface TenantView {
  readonly id: string;
  readonly tenantKey: string;
  readonly name: string;
  readonly active: boolean;
  readonly defaultTenant: boolean;
  readonly createdAt: string;
}

/**
 * Iteration 56 (CVM-106): Read-only Mandanten-Liste fuer die
 * Admin-UI.
 */
@Injectable({ providedIn: 'root' })
export class TenantsService {
  private readonly api = inject(ApiClient);

  list(): Promise<TenantView[]> {
    return firstValueFrom(
      this.api.get<TenantView[]>('/api/v1/admin/tenants')
    );
  }

  /** Iteration 59 (CVM-109): Neuer Mandant. */
  create(req: TenantCreateRequest): Promise<TenantView> {
    return firstValueFrom(
      this.api.post<TenantView, TenantCreateRequest>(
        '/api/v1/admin/tenants',
        req
      )
    );
  }
}

export interface TenantCreateRequest {
  readonly tenantKey: string;
  readonly name: string;
  readonly active?: boolean | null;
}
