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
      hint="CVE-Detailansicht im Backlog. Daten kommen aus den NVD-/GHSA-/KEV-/EPSS-Feeds (Iteration 03); UI folgt mit der CVE-Inventar-Iteration."
    ></ahs-empty-state>
  `
})
export class CvesComponent {}
