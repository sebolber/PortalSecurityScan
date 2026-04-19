import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AhsCardComponent } from '../../shared/components/ahs-card.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { AuthService } from '../../core/auth/auth.service';
import {
  AhsSeverity,
  ReportResponse,
  ReportsService
} from '../../core/reports/reports.service';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import {
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';

interface ProduktVersionOption {
  readonly id: string;
  readonly label: string;
}

/**
 * Reports-UI (Iteration 10 Nachzug):
 * <ul>
 *   <li>Form zum Anlegen eines Hardening-Reports.</li>
 *   <li>Liste der in dieser Session erzeugten Reports
 *       (Backend hat keinen Listing-Endpoint - bleibt offen).</li>
 *   <li>Download via Blob -> {@code window.open}.</li>
 * </ul>
 */
@Component({
  selector: 'cvm-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    AhsCardComponent,
    SeverityBadgeComponent,
    CvmIconComponent
  ],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  private readonly reports = inject(ReportsService);
  private readonly auth = inject(AuthService);
  private readonly products = inject(ProductsService);
  private readonly environments = inject(EnvironmentsService);

  readonly severities: readonly AhsSeverity[] = [
    'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFORMATIONAL', 'NOT_APPLICABLE'
  ];

  productVersionId = '';
  environmentId = '';
  gesamteinstufung: AhsSeverity = 'MEDIUM';
  freigeberKommentar = '';

  readonly erzeugte = signal<readonly ReportResponse[]>([]);
  readonly busy = signal<boolean>(false);
  readonly fehler = signal<string | null>(null);

  /**
   * UI-Fix MEDIUM-5 (UI-Exploration 20260418): Statt UUIDs per Hand
   * eintippen zu lassen, laden wir Produkt-Versionen und Umgebungen
   * aus dem Backend und zeigen sie im Dropdown. Scheitert das Laden
   * (z. B. fehlende Rolle), bieten wir als Fallback ein Textfeld an.
   */
  readonly produktVersionen = signal<readonly ProduktVersionOption[]>([]);
  readonly umgebungen = signal<readonly EnvironmentView[]>([]);
  readonly ladeKatalog = signal<boolean>(false);
  readonly katalogFehler = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    this.ladeKatalog.set(true);
    this.katalogFehler.set(null);
    try {
      const [produkte, envs] = await Promise.all([
        this.products.list(),
        this.environments.list()
      ]);
      const versionen = await this.sammleVersionen(produkte);
      this.produktVersionen.set(versionen);
      this.umgebungen.set(envs);
    } catch (err) {
      this.katalogFehler.set(
        err instanceof Error && err.message
          ? err.message
          : 'Produkt-/Umgebungsliste konnte nicht geladen werden. '
              + 'UUIDs bitte manuell eintragen.'
      );
    } finally {
      this.ladeKatalog.set(false);
    }
  }

  private async sammleVersionen(
    produkte: readonly ProductView[]
  ): Promise<ProduktVersionOption[]> {
    const alle: ProduktVersionOption[] = [];
    for (const produkt of produkte) {
      let vs: readonly ProductVersionView[] = [];
      try {
        vs = await this.products.versions(produkt.id);
      } catch {
        // einzelne Produkte duerfen scheitern (z. B. Soft-Deletes)
        continue;
      }
      for (const v of vs) {
        alle.push({
          id: v.id,
          label: produkt.name + ' ' + v.version
            + (v.gitCommit ? ' (' + v.gitCommit.slice(0, 7) + ')' : '')
        });
      }
    }
    return alle;
  }

  async erzeuge(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.productVersionId.trim() || !this.environmentId.trim()) {
      this.fehler.set('productVersionId und environmentId sind Pflicht (UUID).');
      return;
    }
    const erzeugtVon = this.auth.username() || 'unbekannt';
    this.busy.set(true);
    this.fehler.set(null);
    try {
      const response = await firstValueFrom(this.reports.erzeuge({
        productVersionId: this.productVersionId.trim(),
        environmentId: this.environmentId.trim(),
        gesamteinstufung: this.gesamteinstufung,
        freigeberKommentar: this.freigeberKommentar.trim() || undefined,
        erzeugtVon
      }));
      this.erzeugte.update((list) => [response, ...list]);
      this.freigeberKommentar = '';
    } catch (err) {
      this.fehler.set(this.formatError(err));
    } finally {
      this.busy.set(false);
    }
  }

  async download(report: ReportResponse): Promise<void> {
    try {
      const blob = await firstValueFrom(this.reports.ladePdf(report.reportId));
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `hardening-report-${report.reportId}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch (err) {
      this.fehler.set(this.formatError(err));
    }
  }

  private formatError(err: unknown): string {
    if (err && typeof err === 'object') {
      const obj = err as Record<string, unknown>;
      const status = typeof obj['status'] === 'number' ? obj['status'] : null;
      if (status === 401 || status === 403) {
        return 'Keine Berechtigung. Bitte als Freigeber anmelden.';
      }
      if (status === 400) {
        return 'Eingaben unvollstaendig - sind Produkt-Version und Umgebung gueltige UUIDs?';
      }
      if (status && status >= 500) {
        return 'Serverfehler beim Erzeugen des Reports.';
      }
    }
    return 'Unbekannter Fehler beim Report-Vorgang.';
  }
}
