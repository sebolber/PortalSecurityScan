import { Component } from '@angular/core';
import { PagePlaceholderComponent } from '../../shared/components/page-placeholder.component';

@Component({
  selector: 'cvm-anomaly',
  standalone: true,
  imports: [PagePlaceholderComponent],
  template: `
    <cvm-page-placeholder
      title="Anomalie-Board"
      description="Gestoppte KI-Vorbewertungen und Profil-Assistenten-Dialog folgen hier."
      iteration="Iteration 27b"
      ticket="CVM-62"
    ></cvm-page-placeholder>
  `
})
export class AnomalyComponent {}
