import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-tenant-kpi',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Mandanten-Dashboard"
      description="Cross-Tenant-KPIs mit Row-Level-Security-Beachtung folgen hier."
      iteration="Iteration 27b"
      ticket="CVM-62"
    ></cvm-page-placeholder>
  `
})
export class TenantKpiComponent {}
