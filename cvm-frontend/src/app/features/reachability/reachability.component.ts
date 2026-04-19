import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ReachabilityQueryService,
  ReachabilityResult,
  ReachabilitySummaryView
} from '../../core/reachability/reachability.service';
import { AuthService } from '../../core/auth/auth.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import {
  ReachabilityStartDialogComponent,
  ReachabilityStartDialogInput
} from './reachability-start-dialog.component';

/**
 * Reachability-Uebersicht (Iteration 27e, CVM-65). Iteration 61
 * (CVM-62): Migration von Angular Material auf pure Tailwind-
 * Komponenten. Der Start-Dialog wird inline ueber {@code <cvm-dialog>}
 * gerendert und durch ein Signal gesteuert.
 */
@Component({
  selector: 'cvm-reachability',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DatePipe,
    DecimalPipe,
    AhsBannerComponent,
    EmptyStateComponent,
    CvmIconComponent,
    ReachabilityStartDialogComponent
  ],
  templateUrl: './reachability.component.html',
  styleUrls: ['./reachability.component.scss']
})
export class ReachabilityComponent implements OnInit {
  private readonly api = inject(ReachabilityQueryService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(CvmToastService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly ReachabilitySummaryView[]>([]);

  // Inline-Dialog-Steuerung (Iteration 61, CVM-62).
  readonly startDialogOffen = signal(false);
  readonly startDialogData = signal<ReachabilityStartDialogInput | null>(null);
  readonly neueFindingId = signal<string>('');

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.api.list(50));
    } catch {
      this.error.set('Reachability-Analysen konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  trackId(_: number, r: ReachabilitySummaryView): string {
    return r.id;
  }

  oeffneStartDialog(): void {
    const findingId = this.neueFindingId().trim();
    if (!findingId) {
      this.toast.warning('Bitte Finding-ID eintragen.');
      return;
    }
    const triggeredBy = this.auth.username() || 'anonymous';
    this.startDialogData.set({ findingId, triggeredBy });
    this.startDialogOffen.set(true);
  }

  onDialogConfirm(result: ReachabilityResult): void {
    this.startDialogOffen.set(false);
    this.startDialogData.set(null);
    this.neueFindingId.set('');
    if (!result.available) {
      const hinweis = result.noteIfUnavailable?.trim()
        || 'Kein Detail vom Backend gemeldet.';
      this.toast.warning('Reachability nicht verfuegbar: ' + hinweis);
      return;
    }
    const kurz = (result.summary && result.summary.trim().length > 0)
      ? result.summary.trim()
      : (result.recommendation ?? 'Analyse abgeschlossen.');
    this.toast.success('Reachability fertig: ' + kurz, 8000);
    void this.laden();
  }

  onDialogCancel(): void {
    this.startDialogOffen.set(false);
    this.startDialogData.set(null);
  }
}
