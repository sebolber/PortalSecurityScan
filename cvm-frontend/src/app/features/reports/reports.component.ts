import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AhsCardComponent } from '../../shared/components/ahs-card.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { AuthService } from '../../core/auth/auth.service';
import {
  AhsSeverity,
  ReportResponse,
  ReportsService
} from '../../core/reports/reports.service';

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
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    AhsCardComponent,
    SeverityBadgeComponent
  ],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent {
  private readonly reports = inject(ReportsService);
  private readonly auth = inject(AuthService);

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

  readonly displayedColumns = ['title', 'gesamteinstufung', 'erzeugtAm', 'sha', 'actions'];

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
