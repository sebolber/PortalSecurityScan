import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-fix-verification',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Fix-Verifikation"
      description="Pro Mitigation existiert bereits GET /api/v1/mitigations/{id}/verification. Diese Uebersichtsseite fasst die Quality-Grades (A/B/C) aller offenen Fixes mandantenweit zusammen und wird in einer Folge-Iteration angebunden."
      iteration="Iteration 27e"
      ticket="CVM-65"
    ></cvm-page-placeholder>
  `
})
export class FixVerificationComponent {}
