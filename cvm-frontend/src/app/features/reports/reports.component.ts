import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-reports',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="description"
      title="Berichte"
      hint="PDF-Reports werden in Iteration 10 angebunden."
    ></ahs-empty-state>
  `
})
export class ReportsComponent {}
