import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface EnvironmentView {
  readonly id: string;
  readonly key: string;
  readonly name: string;
  readonly stage: string;
  readonly tenant: string | null;
  readonly llmModelProfileId: string | null;
}

export interface CreateEnvironmentRequest {
  readonly key: string;
  readonly name: string;
  readonly stage: string;
  readonly tenant: string | null;
}

/** Liest + legt Umgebungen an (Iteration 25 + 28e). */
@Injectable({ providedIn: 'root' })
export class EnvironmentsService {
  private readonly api = inject(ApiClient);

  list(): Promise<EnvironmentView[]> {
    return firstValueFrom(this.api.get<EnvironmentView[]>('/api/v1/environments'));
  }

  create(req: CreateEnvironmentRequest): Promise<EnvironmentView> {
    return firstValueFrom(
      this.api.post<EnvironmentView, CreateEnvironmentRequest>(
        '/api/v1/environments',
        req
      )
    );
  }
}
