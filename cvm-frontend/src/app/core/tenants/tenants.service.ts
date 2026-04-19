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

  /** Iteration 60 (CVM-110): Mandant aktivieren/deaktivieren. */
  setActive(tenantId: string, active: boolean): Promise<TenantView> {
    return firstValueFrom(
      this.api.patch<TenantView, { active: boolean }>(
        '/api/v1/admin/tenants/' + tenantId + '/active',
        { active }
      )
    );
  }

  /**
   * Iteration 62 (CVM-62): Setzt den Mandanten als Default.
   * Der bisherige Default wird zurueckgesetzt.
   */
  setDefault(tenantId: string): Promise<TenantView> {
    return firstValueFrom(
      this.api.post<TenantView, Record<string, never>>(
        '/api/v1/admin/tenants/' + tenantId + '/default',
        {}
      )
    );
  }

  /**
   * Iteration 62 (CVM-62): Aktuell eingeloggter Mandant (aus JWT
   * abgeleitet). Liefert null, wenn kein Mandant zugeordnet ist.
   */
  current(): Promise<TenantView | null> {
    return firstValueFrom(
      this.api.getOptional<TenantView>('/api/v1/tenant/current', [204, 404])
    );
  }
}

export interface TenantCreateRequest {
  readonly tenantKey: string;
  readonly name: string;
  readonly active?: boolean | null;
}
