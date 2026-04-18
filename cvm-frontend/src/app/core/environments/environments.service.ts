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

/** Liest die Umgebungsliste (GET /api/v1/environments). */
@Injectable({ providedIn: 'root' })
export class EnvironmentsService {
  private readonly api = inject(ApiClient);

  list(): Promise<EnvironmentView[]> {
    return firstValueFrom(this.api.get<EnvironmentView[]>('/api/v1/environments'));
  }
}
