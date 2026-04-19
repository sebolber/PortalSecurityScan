import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import {
  OnboardingService,
  OnboardingStepId
} from './onboarding.service';

interface StepView {
  readonly id: OnboardingStepId;
  readonly titel: string;
  readonly beschreibung: string;
  readonly ctaLabel: string;
  readonly ctaRoute: string;
  readonly done: boolean;
  readonly current: boolean;
}

const STEP_SPEC: ReadonlyArray<Omit<StepView, 'done' | 'current'>> = [
  {
    id: 'produkt',
    titel: '1. Produkt anlegen',
    beschreibung:
      'Lege das zu scannende Produkt an (z.B. PortalCore-Test). '
      + 'Die Produkt-Id referenziert spaeter alle Versionen, Scans und '
      + 'Bewertungen.',
    ctaLabel: 'Zur Produkt-Anlage',
    ctaRoute: '/admin/products'
  },
  {
    id: 'umgebung',
    titel: '2. Umgebung anlegen',
    beschreibung:
      'Definiere eine Umgebung (z.B. REF-TEST, PROD). Profile und '
      + 'Risk-Schwellen sind pro Umgebung konfigurierbar.',
    ctaLabel: 'Zur Umgebungs-Anlage',
    ctaRoute: '/admin/environments'
  },
  {
    id: 'profil',
    titel: '3. Kontext-Profil hinterlegen',
    beschreibung:
      'Das Profil beschreibt die Angriffsflaeche, ignorierbare '
      + 'Komponenten und Risk-Schwellen fuer die gewaehlte Umgebung. '
      + 'Ohne aktives Profil sind Bewertungen nicht automatisiert.',
    ctaLabel: 'Profile oeffnen',
    ctaRoute: '/profiles'
  },
  {
    id: 'scan',
    titel: '4. Ersten Scan hochladen',
    beschreibung:
      'Lade einen CycloneDX-SBOM fuer die Produkt-Version und '
      + 'Umgebung hoch. Das System ermittelt automatisch CVEs und '
      + 'legt Bewertungsvorschlaege in der Queue an.',
    ctaLabel: 'Zum Scan-Upload',
    ctaRoute: '/scans/upload'
  }
];

/**
 * Iteration 96 (CVM-336): Erstnutzer-Wizard. Vier Schritte, die
 * den Admin durch Produkt -> Umgebung -> Profil -> Scan fuehren.
 * Jeder Schritt ist ein Deep-Link auf die jeweilige Seite plus ein
 * "Als erledigt markieren"-Button. Der Fortschritt lebt in
 * localStorage (siehe {@link OnboardingService}).
 */
@Component({
  selector: 'cvm-onboarding',
  standalone: true,
  imports: [CommonModule, RouterLink, CvmIconComponent],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.scss']
})
export class OnboardingComponent {
  private readonly service = inject(OnboardingService);

  readonly state = this.service.state;

  readonly schritte = computed<readonly StepView[]>(() => {
    const s = this.state();
    return STEP_SPEC.map((spec) => ({
      ...spec,
      done: s.done.includes(spec.id),
      current: s.current === spec.id
    }));
  });

  readonly fortschritt = computed(() => {
    const erledigt = this.state().done.length;
    return Math.round((erledigt / STEP_SPEC.length) * 100);
  });

  readonly fertig = computed(() => this.service.completed());

  markiereErledigt(step: OnboardingStepId): void {
    this.service.markDone(step);
  }

  zuruecksetzen(): void {
    this.service.reset();
  }
}
