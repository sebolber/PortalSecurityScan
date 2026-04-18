import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-waivers',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Waiver-Verwaltung"
      description="Liste aller bestaetigten Waiver mit validUntil-Ablauf-Warner und VEX-Export folgt hier."
      iteration="Iteration 27b"
      ticket="CVM-62"
    ></cvm-page-placeholder>
  `
})
export class WaiversComponent {}
