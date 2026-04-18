import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-reachability',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Reachability-Ergebnisse"
      description="Call-Graph-Evidenz, JGit-Commit-Referenz und Sandbox-Log-Auszug je Finding folgen hier."
      iteration="Iteration 27b"
      ticket="CVM-62"
    ></cvm-page-placeholder>
  `
})
export class ReachabilityComponent {}
