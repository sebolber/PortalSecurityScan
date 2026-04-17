import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-ai-audit',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="fact_check"
      title="KI-Audit"
      hint="ai_call_audit-Sicht folgt mit Iteration 11 (LLM-Gateway)."
    ></ahs-empty-state>
  `
})
export class AiAuditComponent {}
