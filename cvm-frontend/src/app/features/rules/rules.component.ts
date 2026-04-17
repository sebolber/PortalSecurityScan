import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-rules',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="gavel"
      title="Regeln"
      hint="Regel-Editor folgt mit Iteration 17 (KI-Regel-Extraktion)."
    ></ahs-empty-state>
  `
})
export class RulesComponent {}
