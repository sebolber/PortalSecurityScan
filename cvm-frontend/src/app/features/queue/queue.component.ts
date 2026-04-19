import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { QueueStore } from './queue-store';
import { QueueShortcutsDirective } from './queue-shortcuts.directive';
import { QueueFilterBarComponent } from './queue-filter-bar.component';
import { QueueTableComponent } from './queue-table.component';
import { QueueDetailComponent } from './queue-detail.component';
import { QueueHelpOverlayComponent } from './queue-help-overlay.component';
import { AuthService } from '../../core/auth/auth.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { Severity } from '../../shared/components/severity-badge.component';
import {
  AssessmentStatus as QueueFilterStatus,
  QueueEntry,
  RejectCommand,
  ApproveCommand
} from './queue.types';

/**
 * Queue-Seite. Koordiniert {@link QueueStore}, Polling,
 * Tastatur-Shortcuts und die Slide-In-Detailansicht.
 *
 * Iteration 61 (CVM-62): Material entfernt, reines Tailwind. Toast-
 * Feedback via {@code CvmToastService} (im Detail-Panel konsumiert).
 */
@Component({
  selector: 'cvm-queue',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CvmIconComponent,
    QueueShortcutsDirective,
    QueueFilterBarComponent,
    QueueTableComponent,
    QueueDetailComponent,
    QueueHelpOverlayComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './queue.component.html'
})
export class QueueComponent implements OnInit {
  private readonly store = inject(QueueStore);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly entries = this.store.entries;
  readonly selected = this.store.selected;
  readonly loading = this.store.loading;
  readonly error = this.store.error;
  readonly checkedIds = this.store.checkedIds;
  readonly pendingIds = computed<ReadonlySet<string>>(() => this.store.pending());

  readonly helpVisible = signal(false);
  readonly banner = signal<string | null>(null);

  readonly selectedId = this.store.selectedId;

  constructor() {
    // UI-Fix HIGH-4 (UI-Exploration 20260418): Der Banner-Reset ist ein
    // Signal-Write im Effect. allowSignalWrites explizit, damit Angular
    // keinen NG0600 wirft.
    effect(
      () => {
        const aktuell = this.selected();
        if (aktuell) {
          this.banner.set(null);
        }
      },
      { allowSignalWrites: true }
    );

    // Filter-Aenderungen automatisch nachladen. reload() setzt
    // loadingSig/errorSig/entriesSig synchron - ohne allowSignalWrites
    // wirft Angular NG0600 und die Queue-Tabelle bleibt leer.
    effect(
      () => {
        // Abhaengigkeit auf das Filter-Signal registrieren.
        this.store.filter();
        void this.store.reload();
      },
      { allowSignalWrites: true }
    );

    // Iteration 82 (CVM-322): Filter in die URL spiegeln, damit
    // Reload und Share-Links den Zustand erhalten. Wir schreiben
    // replaceUrl, damit der Back-Button nicht jedes Filter-Toggle
    // als History-Schritt sieht.
    effect(
      () => {
        const f = this.store.filter();
        const params = {
          productVersionId: f.productVersionId ?? null,
          environmentId: f.environmentId ?? null,
          status: f.status ?? null,
          source: f.source ?? null
        };
        const current = this.route.snapshot.queryParamMap;
        const same =
          (current.get('productVersionId') ?? null) === params.productVersionId
          && (current.get('environmentId') ?? null) === params.environmentId
          && (current.get('status') ?? null) === params.status
          && (current.get('source') ?? null) === params.source;
        if (!same) {
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: params,
            queryParamsHandling: 'merge',
            replaceUrl: true
          });
        }
      }
    );
  }

  ngOnInit(): void {
    // Iteration 80 (CVM-320): queryParams in Store-Filter uebersetzen,
    // damit Deep-Links wie /queue?productVersionId=... aus Scan-
    // Upload-CTAs direkt filtern.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const productVersionId = params.get('productVersionId') ?? undefined;
        const environmentId = params.get('environmentId') ?? undefined;
        const status = params.get('status') ?? undefined;
        const current = this.store.filter();
        if (productVersionId !== current.productVersionId
            || environmentId !== current.environmentId
            || status !== current.status) {
          this.store.setFilter({
            productVersionId,
            environmentId,
            status: status as QueueFilterStatus | undefined
          });
        }
      });

    // Polling alle 60 s.
    interval(60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.store.reload());
  }

  onSelect(id: string): void {
    this.store.select(id);
  }

  onToggle(id: string): void {
    this.store.toggleChecked(id);
  }

  onReload(): void {
    void this.store.reload();
  }

  onNext(): void {
    this.store.moveSelection(1);
  }

  onPrevious(): void {
    this.store.moveSelection(-1);
  }

  onShortcutApprove(): void {
    const aktuell = this.selected();
    if (!aktuell) {
      this.banner.set('Kein Vorschlag ausgewaehlt. Bewege mit j/k durch die Liste.');
      return;
    }
    void this.approve(aktuell, {
      severity: aktuell.severity,
      rationale: aktuell.rationale ?? '',
      zweitfreigabe: false
    });
  }

  onShortcutReject(): void {
    this.banner.set('Reject erfordert einen Kommentar. Siehe Detail-Panel.');
  }

  onApproveFromDetail(
    entry: QueueEntry,
    payload: {
      severity: string;
      rationale: string;
      strategy?: string;
      targetVersion?: string;
      plannedFor?: string;
      zweitfreigabe: boolean;
    }
  ): void {
    void this.approve(entry, payload);
  }

  onRejectFromDetail(entry: QueueEntry, kommentar: string): void {
    const approverId = this.auth.username() || 'unknown';
    const command: RejectCommand = { approverId, comment: kommentar };
    void this.store.reject(entry.id, command);
  }

  onCloseDetail(): void {
    this.store.select(null);
  }

  openHelp(): void {
    this.helpVisible.set(true);
  }

  closeHelp(): void {
    this.helpVisible.set(false);
  }

  dismissBanner(): void {
    this.banner.set(null);
  }

  private async approve(
    entry: QueueEntry,
    payload: {
      severity: string;
      rationale: string;
      strategy?: string;
      targetVersion?: string;
      plannedFor?: string;
      zweitfreigabe: boolean;
    }
  ): Promise<void> {
    const approverId = this.auth.username() || 'unknown';
    const command: ApproveCommand = {
      approverId,
      severity: payload.severity as Severity,
      strategy: payload.strategy,
      targetVersion: payload.targetVersion,
      plannedFor: payload.plannedFor
    };
    const ok = await this.store.approve(entry.id, command);
    if (!ok && payload.zweitfreigabe) {
      this.banner.set(
        'Zweitfreigabe erforderlich: Vorschlag kann nicht vom gleichen Benutzer '
          + 'freigegeben werden. Bitte einen CVM_APPROVER bitten.'
      );
    }
  }
}
