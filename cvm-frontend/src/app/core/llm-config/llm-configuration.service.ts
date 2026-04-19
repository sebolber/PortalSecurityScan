import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface LlmConfigurationView {
  readonly id: string;
  readonly tenantId: string;
  readonly name: string;
  readonly description: string | null;
  readonly provider: string;
  readonly model: string;
  readonly baseUrl: string | null;
  readonly secretSet: boolean;
  readonly secretHint: string | null;
  readonly maxTokens: number | null;
  readonly temperature: number | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly updatedBy: string | null;
}

export interface LlmProviderInfo {
  readonly provider: string;
  readonly defaultBaseUrl: string | null;
  readonly requiresExplicitBaseUrl: boolean;
}

export interface LlmConfigurationCreateRequest {
  readonly name: string;
  readonly description: string | null;
  readonly provider: string;
  readonly model: string;
  readonly baseUrl: string | null;
  readonly secret: string | null;
  readonly maxTokens: number | null;
  readonly temperature: number | null;
  readonly active: boolean;
}

export interface LlmConfigurationUpdateRequest {
  readonly name?: string | null;
  readonly description?: string | null;
  readonly provider?: string | null;
  readonly model?: string | null;
  readonly baseUrl?: string | null;
  readonly secret?: string | null;
  readonly secretClear?: boolean | null;
  readonly maxTokens?: number | null;
  readonly temperature?: number | null;
  readonly active?: boolean | null;
}

/**
 * Frontend-Service fuer die LLM-Konfigurationen (Iteration 34b,
 * CVM-78). Kapselt die REST-Aufrufe gegen
 * {@code /api/v1/admin/llm-configurations} und stellt Ergebnisse als
 * Promises zur Verfuegung.
 */
@Injectable({ providedIn: 'root' })
export class LlmConfigurationService {
  private readonly api = inject(ApiClient);
  private readonly basePath = '/api/v1/admin/llm-configurations';

  list(): Promise<LlmConfigurationView[]> {
    return firstValueFrom(
      this.api.get<LlmConfigurationView[]>(this.basePath)
    );
  }

  providers(): Promise<LlmProviderInfo[]> {
    return firstValueFrom(
      this.api.get<LlmProviderInfo[]>(this.basePath + '/providers')
    );
  }

  byId(id: string): Promise<LlmConfigurationView> {
    return firstValueFrom(
      this.api.get<LlmConfigurationView>(this.basePath + '/' + id)
    );
  }

  create(
    request: LlmConfigurationCreateRequest
  ): Promise<LlmConfigurationView> {
    return firstValueFrom(
      this.api.post<LlmConfigurationView, LlmConfigurationCreateRequest>(
        this.basePath,
        request
      )
    );
  }

  update(
    id: string,
    request: LlmConfigurationUpdateRequest
  ): Promise<LlmConfigurationView> {
    return firstValueFrom(
      this.api.put<LlmConfigurationView, LlmConfigurationUpdateRequest>(
        this.basePath + '/' + id,
        request
      )
    );
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(
      this.api.delete<void>(this.basePath + '/' + id)
    );
  }
}
