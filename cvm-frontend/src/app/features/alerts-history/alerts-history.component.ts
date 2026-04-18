import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-alerts-history',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Alert-Historie"
      description="Bisherige E-Mail-Alerts mit Empfaenger, Cooldown-Status und Eskalationsstufe folgen hier."
      iteration="Iteration 27b"
      ticket="CVM-62"
    ></cvm-page-placeholder>
  `
})
export class AlertsHistoryComponent {}
