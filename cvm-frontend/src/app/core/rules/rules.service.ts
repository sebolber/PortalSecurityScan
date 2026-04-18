import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface RuleResponse {
  readonly id: string;
  readonly ruleKey: string;
  readonly name: string;
  readonly description: string | null;
  readonly status: 'DRAFT' | 'ACTIVE' | 'RETIRED' | string;
  readonly proposedSeverity: string;
  readonly conditionJson: string;
  readonly rationaleTemplate: string | null;
  readonly rationaleSourceFields: readonly string[];
  readonly origin: string | null;
  readonly version: number;
  readonly createdBy: string;
  readonly createdAt: string;
  readonly activatedBy: string | null;
  readonly activatedAt: string | null;
}

export interface RuleActivateRequest {
  readonly approverId: string;
}

export interface DryRunResponse {
  readonly ruleId: string;
  readonly fensterTage: number;
  readonly wuerdeGreifen: number;
  readonly kollidiertMitAktivBewertung: number;
  readonly beispiele: readonly string[];
}

/** Thin HTTP-Wrapper um die Rules-Endpunkte aus Iteration 05/23. */
@Injectable({ providedIn: 'root' })
export class RulesService {
  private readonly api = inject(ApiClient);

  list(): Promise<RuleResponse[]> {
    return firstValueFrom(this.api.get<RuleResponse[]>('/api/v1/rules'));
  }

  activate(ruleId: string, approverId: string): Promise<RuleResponse> {
    return firstValueFrom(this.api.post<RuleResponse, RuleActivateRequest>(
      `/api/v1/rules/${ruleId}/activate`,
      { approverId }
    ));
  }

  dryRun(ruleId: string, days = 180): Promise<DryRunResponse> {
    return firstValueFrom(this.api.post<DryRunResponse, Record<string, never>>(
      `/api/v1/rules/${ruleId}/dry-run?days=${days}`,
      {}
    ));
  }
}
