import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface ModelProfileView {
  readonly id: string;
  readonly profileKey: string;
  readonly provider: 'CLAUDE_CLOUD' | 'OLLAMA_ONPREM' | string;
  readonly modelId: string;
  readonly modelVersion: string | null;
  readonly costBudgetEurMonthly: number;
  readonly approvedForGkvData: boolean;
}

export interface ProfileSwitchRequest {
  readonly newProfileId: string;
  readonly changedBy: string;
  readonly fourEyesConfirmer: string;
  readonly reason: string | null;
}

export interface ProfileSwitchResponse {
  readonly id: string;
  readonly environmentId: string;
  readonly previousProfileId: string | null;
  readonly newProfileId: string;
  readonly changedBy: string;
  readonly fourEyesConfirmer: string;
  readonly reason: string | null;
  readonly changedAt: string;
}

/**
 * Modellprofile lesen (GET /api/v1/llm-model-profiles) und
 * pro Umgebung umschalten (POST /environments/{id}/model-profile/switch).
 */
@Injectable({ providedIn: 'root' })
export class ModelProfileService {
  private readonly api = inject(ApiClient);

  list(): Promise<ModelProfileView[]> {
    return firstValueFrom(this.api.get<ModelProfileView[]>('/api/v1/llm-model-profiles'));
  }

  switch(environmentId: string, req: ProfileSwitchRequest): Promise<ProfileSwitchResponse> {
    return firstValueFrom(this.api.post<ProfileSwitchResponse, ProfileSwitchRequest>(
      `/api/v1/environments/${environmentId}/model-profile/switch`,
      req
    ));
  }
}
