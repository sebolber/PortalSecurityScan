import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-reachability',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Reachability-Ergebnisse"
      description="Einstieg: Assessment-Queue oeffnen, Finding waehlen und 'Reachability starten' ausloesen. Diese Uebersichtsseite fasst die erzeugten Analysen mandantenweit zusammen und wird in einer Folge-Iteration angebunden (Backend-Endpunkt POST /api/v1/findings/{id}/reachability existiert bereits)."
      iteration="Iteration 27e"
      ticket="CVM-65"
    ></cvm-page-placeholder>
  `
})
export class ReachabilityComponent {}
