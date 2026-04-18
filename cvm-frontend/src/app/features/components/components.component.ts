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
      hint="Komponenteninventar im Backlog. Aktuell sind die Daten aus den SBOM-Imports (Iteration 02) nur ueber Backend-Queries lesbar; UI folgt zusammen mit der CVE-Sicht."
    ></ahs-empty-state>
  `
})
export class ComponentsComponent {}
