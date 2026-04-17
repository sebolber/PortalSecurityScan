import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-queue',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="rule"
      title="Bewertungs-Queue"
      hint="Die Queue wird in Iteration 08 (CVM-17) gegen das Backend verdrahtet."
    ></ahs-empty-state>
  `
})
export class QueueComponent {}
