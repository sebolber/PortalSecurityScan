import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-cves',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="bug_report"
      title="CVEs"
      hint="CVE-Detailansicht folgt im Anschluss an die Queue-UI."
    ></ahs-empty-state>
  `
})
export class CvesComponent {}
