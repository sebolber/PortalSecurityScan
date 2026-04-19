import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { AuthService } from '../../core/auth/auth.service';
import {
  WaiverStatus,
  WaiverView,
  WaiversService
} from '../../core/waivers/waivers.service';

const WAIVER_STATUSSE: readonly WaiverStatus[] = [
  'ACTIVE',
  'EXPIRED',
  'REVOKED'
];

/**
 * Waiver-Basisansicht (Iteration 27c, CVM-63). Ersetzt den
 * Platzhalter aus 27b durch eine Server-gespeiste Liste mit
 * Status-Filter und validUntil-Ablauf-Warner.
 *
 * Iteration 85 (CVM-325): Extend- und Revoke-Aktionen mit
 * cvm-dialog und Vier-Augen-Warnung.
 */
@Component({
  selector: 'cvm-waivers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    AhsBannerComponent,
    CvmIconComponent,
    CvmDialogComponent,
    EmptyStateComponent,
    DatePipe
  ],
  templateUrl: './waivers.component.html',
  styleUrls: ['./waivers.component.scss']
})
export class WaiversComponent implements OnInit {
  private readonly waivers = inject(WaiversService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(CvmToastService);

  readonly statusse = WAIVER_STATUSSE;

  status = signal<WaiverStatus>('ACTIVE');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly WaiverView[]>([]);

  // Iteration 85 (CVM-325): Dialog-Steuerung.
  readonly extendDialogOffen = signal(false);
  readonly revokeDialogOffen = signal(false);
  readonly aktiverWaiver = signal<WaiverView | null>(null);
  readonly extendValidUntil = signal<string>('');
  readonly revokeReason = signal<string>('');
  readonly pending = signal(false);

  readonly abgelaufenBald = computed(() =>
    this.rows().filter((w) => this.isBaldAblaufend(w))
  );

  readonly extendVierAugenKonflikt = computed(() => {
    const w = this.aktiverWaiver();
    const user = this.auth.username();
    return !!(w && user && w.grantedBy === user);
  });

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const list = await this.waivers.list(this.status());
      this.rows.set(list);
    } catch {
      this.error.set('Waiver-Liste konnte nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  statusWechseln(value: WaiverStatus): void {
    this.status.set(value);
    void this.laden();
  }

  isBaldAblaufend(waiver: WaiverView): boolean {
    if (waiver.status !== 'ACTIVE' || !waiver.validUntil) {
      return false;
    }
    const diff = new Date(waiver.validUntil).getTime() - Date.now();
    const tage = diff / (1000 * 60 * 60 * 24);
    return tage >= 0 && tage <= 14;
  }

  trackId(_: number, w: WaiverView): string {
    return w.id;
  }

  oeffneExtendDialog(w: WaiverView): void {
    this.aktiverWaiver.set(w);
    const d = new Date();
    d.setDate(d.getDate() + 90);
    this.extendValidUntil.set(d.toISOString().slice(0, 10));
    this.extendDialogOffen.set(true);
  }

  async extendBestaetigen(): Promise<void> {
    const w = this.aktiverWaiver();
    if (!w) {
      return;
    }
    if (this.extendVierAugenKonflikt()) {
      return;
    }
    const dateStr = this.extendValidUntil();
    if (!dateStr) {
      this.toast.warning('Bitte ein neues Gueltig-bis-Datum waehlen.');
      return;
    }
    const extendedBy = this.auth.username() || 'unbekannt';
    const isoTime = new Date(dateStr + 'T00:00:00Z').toISOString();
    this.pending.set(true);
    try {
      await this.waivers.extend(w.id, isoTime, extendedBy);
      this.toast.success('Waiver verlaengert.', 4000);
      this.extendDialogOffen.set(false);
      this.aktiverWaiver.set(null);
      await this.laden();
    } catch {
      this.toast.error('Verlaengerung fehlgeschlagen.');
    } finally {
      this.pending.set(false);
    }
  }

  oeffneRevokeDialog(w: WaiverView): void {
    this.aktiverWaiver.set(w);
    this.revokeReason.set('');
    this.revokeDialogOffen.set(true);
  }

  async revokeBestaetigen(): Promise<void> {
    const w = this.aktiverWaiver();
    if (!w) {
      return;
    }
    const reason = this.revokeReason().trim();
    if (!reason) {
      this.toast.warning('Bitte Begruendung eintragen.');
      return;
    }
    const revokedBy = this.auth.username() || 'unbekannt';
    this.pending.set(true);
    try {
      await this.waivers.revoke(w.id, revokedBy, reason);
      this.toast.success('Waiver widerrufen.', 4000);
      this.revokeDialogOffen.set(false);
      this.aktiverWaiver.set(null);
      this.revokeReason.set('');
      await this.laden();
    } catch {
      this.toast.error('Widerruf fehlgeschlagen.');
    } finally {
      this.pending.set(false);
    }
  }

  abbrechen(): void {
    this.extendDialogOffen.set(false);
    this.revokeDialogOffen.set(false);
    this.aktiverWaiver.set(null);
    this.revokeReason.set('');
  }
}
