import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export type SystemParameterType =
  | 'STRING'
  | 'INTEGER'
  | 'DECIMAL'
  | 'BOOLEAN'
  | 'EMAIL'
  | 'URL'
  | 'JSON'
  | 'PASSWORD'
  | 'SELECT'
  | 'MULTISELECT'
  | 'DATE'
  | 'TIMESTAMP'
  | 'TEXTAREA'
  | 'HOST'
  | 'IP';

export interface SystemParameterView {
  readonly id: string;
  readonly paramKey: string;
  readonly label: string;
  readonly description: string | null;
  readonly handbook: string | null;
  readonly category: string;
  readonly subcategory: string | null;
  readonly type: SystemParameterType;
  readonly value: string | null;
  readonly defaultValue: string | null;
  readonly required: boolean;
  readonly validationRules: string | null;
  readonly options: string | null;
  readonly unit: string | null;
  readonly sensitive: boolean;
  readonly hotReload: boolean;
  readonly validFrom: string | null;
  readonly validTo: string | null;
  readonly adminOnly: boolean;
  readonly createdAt: string;
  readonly createdBy: string | null;
  readonly updatedAt: string;
  readonly updatedBy: string | null;
}

export interface SystemParameterAuditLogView {
  readonly id: string;
  readonly parameterId: string;
  readonly paramKey: string;
  readonly oldValue: string | null;
  readonly newValue: string | null;
  readonly changedBy: string | null;
  readonly changedAt: string;
  readonly reason: string | null;
}

export interface SystemParameterCreateRequest {
  readonly paramKey: string;
  readonly label: string;
  readonly description?: string | null;
  readonly handbook?: string | null;
  readonly category: string;
  readonly subcategory?: string | null;
  readonly type: SystemParameterType;
  readonly value?: string | null;
  readonly defaultValue?: string | null;
  readonly required: boolean;
  readonly validationRules?: string | null;
  readonly options?: string | null;
  readonly unit?: string | null;
  readonly sensitive: boolean;
  readonly hotReload: boolean;
  readonly validFrom?: string | null;
  readonly validTo?: string | null;
  readonly adminOnly: boolean;
}

export interface SystemParameterUpdateRequest {
  readonly label: string;
  readonly description?: string | null;
  readonly handbook?: string | null;
  readonly category: string;
  readonly subcategory?: string | null;
  readonly type: SystemParameterType;
  readonly defaultValue?: string | null;
  readonly required: boolean;
  readonly validationRules?: string | null;
  readonly options?: string | null;
  readonly unit?: string | null;
  readonly sensitive: boolean;
  readonly hotReload: boolean;
  readonly validFrom?: string | null;
  readonly validTo?: string | null;
  readonly adminOnly: boolean;
}

export interface SystemParameterChangeValueRequest {
  readonly value: string | null;
  readonly reason: string | null;
}

/**
 * Frontend-Service fuer den System-Parameter-Dialog. Kapselt die
 * REST-Aufrufe gegen {@code /api/v1/admin/parameters}.
 */
@Injectable({ providedIn: 'root' })
export class SystemParameterService {
  private readonly api = inject(ApiClient);
  private readonly basePath = '/api/v1/admin/parameters';

  list(category?: string | null): Promise<SystemParameterView[]> {
    const suffix = category ? '?category=' + encodeURIComponent(category) : '';
    return firstValueFrom(
      this.api.get<SystemParameterView[]>(this.basePath + suffix)
    );
  }

  byId(id: string): Promise<SystemParameterView> {
    return firstValueFrom(
      this.api.get<SystemParameterView>(this.basePath + '/' + id)
    );
  }

  create(request: SystemParameterCreateRequest): Promise<SystemParameterView> {
    return firstValueFrom(
      this.api.post<SystemParameterView, SystemParameterCreateRequest>(
        this.basePath,
        request
      )
    );
  }

  update(
    id: string,
    request: SystemParameterUpdateRequest
  ): Promise<SystemParameterView> {
    return firstValueFrom(
      this.api.put<SystemParameterView, SystemParameterUpdateRequest>(
        this.basePath + '/' + id,
        request
      )
    );
  }

  changeValue(
    id: string,
    request: SystemParameterChangeValueRequest
  ): Promise<SystemParameterView> {
    return firstValueFrom(
      this.api.patch<SystemParameterView, SystemParameterChangeValueRequest>(
        this.basePath + '/' + id + '/value',
        request
      )
    );
  }

  reset(id: string): Promise<SystemParameterView> {
    return firstValueFrom(
      this.api.patch<SystemParameterView, Record<string, never>>(
        this.basePath + '/' + id + '/reset',
        {}
      )
    );
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(
      this.api.delete<void>(this.basePath + '/' + id)
    );
  }

  auditLog(parameterId?: string): Promise<SystemParameterAuditLogView[]> {
    const suffix = parameterId
      ? '?parameterId=' + encodeURIComponent(parameterId)
      : '';
    return firstValueFrom(
      this.api.get<SystemParameterAuditLogView[]>(
        this.basePath + '/audit-log' + suffix
      )
    );
  }
}
