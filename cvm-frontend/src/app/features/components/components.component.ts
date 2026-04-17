import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-components',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="inventory_2"
      title="Komponenten"
      hint="Komponenteninventar wird mit der CVE-Ansicht ergaenzt."
    ></ahs-empty-state>
  `
})
export class ComponentsComponent {}
