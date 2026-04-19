import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import {
  ReachabilityQueryService,
  ReachabilityResult,
  ReachabilityStartRequest,
  ReachabilitySuggestion
} from '../../core/reachability/reachability.service';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import {
  Severity,
  SeverityBadgeComponent
} from '../../shared/components/severity-badge.component';
import { QueueEntry, SEVERITY_REIHENFOLGE } from './queue.types';
import { braucheZweitfreigabe } from './vier-augen';

interface ReachabilityFormState {
  repoUrl: string;
  branch: string;
  commitSha: string;
  vulnerableSymbol: string;
  language: string;
  instruction: string;
}

const REACH_STORAGE_KEY = 'cvm.reachability.last';
const REACH_SPRACHEN = [
  'java',
  'javascript',
  'typescript',
  'python',
  'go',
  'rust'
] as const;

function leeresReachabilityFormular(): ReachabilityFormState {
  return {
    repoUrl: '',
    branch: '',
    commitSha: '',
    vulnerableSymbol: '',
    language: 'java',
    instruction: ''
  };
}

function ladeGespeicherteReachability(): ReachabilityFormState | null {
  if (typeof localStorage === 'undefined') {
    return null;
  }
  try {
    const raw = localStorage.getItem(REACH_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<ReachabilityFormState>;
    return {
      ...leeresReachabilityFormular(),
      ...parsed,
      vulnerableSymbol: '',
      instruction: ''
    };
  } catch {
    return null;
  }
}

function speichereReachability(state: ReachabilityFormState): void {
  if (typeof localStorage === 'undefined') {
    return;
  }
  try {
    localStorage.setItem(
      REACH_STORAGE_KEY,
      JSON.stringify({
        repoUrl: state.repoUrl,
        branch: state.branch,
        commitSha: state.commitSha,
        language: state.language
      })
    );
  } catch {
    // Private-Mode o.ae. - stumm verworfen.
  }
}

/**
 * Slide-In-Detailpanel. Haelt lokal editierbare Kopien der
 * Severity/Rationale/Mitigation-Felder. Aktionen werden als Events an
 * den Container (Queue-Komponente) weitergereicht; der Container
 * orchestriert den Store-Aufruf.
 *
 * Iteration 61 (CVM-62): Material entfernt. Reachability-Dialog ist
 * nun inline ueber `cvm-dialog` eingebettet; keine `MatDialog`-
 * Abhaengigkeit mehr. SnackBar -> `CvmToastService`.
 */
@Component({
  selector: 'cvm-queue-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CvmDialogComponent,
    CvmIconComponent,
    SeverityBadgeComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="flex h-full w-full max-w-lg flex-col border-l border-border bg-surface shadow-card"
      role="dialog"
      aria-label="Bewertungs-Detail"
      tabindex="-1"
    >
      @if (entry) {
        <header class="flex items-center justify-between gap-2 border-b border-border p-4">
          <div class="flex flex-col gap-1">
            <div class="text-xs uppercase tracking-wide text-text-muted">
              {{ entry.source }} &middot; v{{ entry.version }}
            </div>
            <a class="font-mono text-sm text-primary hover:underline"
               [routerLink]="['/cves', entry.cveKey]"
               title="Zur CVE-Detailseite">{{ entry.cveKey }}</a>
          </div>
          <button
            type="button"
            class="btn-icon"
            (click)="close.emit()"
            aria-label="Schliessen"
          >
            <cvm-icon name="close" [size]="20"></cvm-icon>
          </button>
        </header>

        <div class="flex-1 space-y-4 overflow-y-auto p-4 text-sm">
          <div class="form-group">
            <span class="form-label">Aktueller Status</span>
            <span class="font-medium">{{ entry.status }}</span>
          </div>

          <div class="form-group">
            <span class="form-label">Original-Severity</span>
            <div>
              <ahs-severity-badge [severity]="entry.severity"></ahs-severity-badge>
            </div>
          </div>

          <label class="form-group">
            <span class="form-label">Vorschlags-Severity (editierbar)</span>
            <select
              class="select-field"
              [(ngModel)]="zielSeverity"
            >
              @for (s of severities; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
            <span class="form-help">
              Aenderung wird beim Klick auf <strong>Freigeben</strong>
              uebernommen. Ein Downgrade auf INFORMATIONAL oder
              NOT_APPLICABLE erfordert Zweitfreigabe.
            </span>
          </label>

          <label class="form-group">
            <span class="form-label">Begruendung</span>
            <textarea
              class="textarea-field"
              [(ngModel)]="rationale"
              placeholder="Warum wird so entschieden?"
            ></textarea>
          </label>

          <fieldset class="rounded-lg border border-border p-3">
            <legend class="px-1 text-xs font-medium uppercase tracking-wide text-text-muted">Mitigation</legend>
            <div class="flex flex-col gap-3">
              <label class="form-group">
                <span class="form-label">Strategie</span>
                <select
                  class="select-field"
                  [(ngModel)]="strategy"
                  name="strategy"
                >
                  <option value="">(keine Angabe)</option>
                  @for (s of mitigationStrategien; track s) {
                    <option [value]="s">{{ s }}</option>
                  }
                </select>
              </label>
              <label class="form-group">
                <span class="form-label">Ziel-Release</span>
                <input
                  type="text"
                  class="input-field"
                  [(ngModel)]="targetVersion"
                  placeholder="z.B. 1.15.0"
                />
              </label>
              <label class="form-group">
                <span class="form-label">Geplant bis</span>
                <input
                  type="date"
                  class="input-field"
                  [(ngModel)]="plannedFor"
                />
              </label>
            </div>
          </fieldset>

          @if (zweitfreigabe) {
            <div class="banner banner-warning" role="status">
              <cvm-icon name="alert-triangle" [size]="18"></cvm-icon>
              <span>
                Zweitfreigabe erforderlich: Downgrade auf {{ zielSeverity }} darf
                nicht von der Person freigegeben werden, die den Vorschlag angelegt hat.
              </span>
            </div>
          }

          @if (selbstfreigabeKonflikt) {
            <!-- Iteration 86 (CVM-326): Vier-Augen-Pre-Check. -->
            <div
              class="banner banner-critical"
              role="alert"
              data-testid="queue-detail-selbstfreigabe-banner"
            >
              <cvm-icon name="alert-circle" [size]="18"></cvm-icon>
              <span>
                Du hast diesen Vorschlag bereits gestellt. Vier-Augen-Prinzip
                verlangt, dass eine andere Person mit Approver-Rolle freigibt.
              </span>
            </div>
          }

          @if (showRejectKommentar) {
            <label class="form-group">
              <span class="form-label form-label--required">Ablehnungs-Kommentar</span>
              <textarea
                class="textarea-field is-invalid"
                [(ngModel)]="rejectKommentar"
              ></textarea>
            </label>
          }
        </div>

        <footer class="flex flex-wrap gap-2 border-t border-border p-4">
          <button
            type="button"
            class="btn btn-primary"
            data-testid="queue-detail-approve"
            [disabled]="pending || selbstfreigabeKonflikt"
            (click)="onApprove()"
          >
            @if (selbstfreigabeKonflikt) {
              Freigabe durch andere Person erforderlich
            } @else {
              {{ zweitfreigabe ? 'Zur Zweitfreigabe einreichen' : 'Freigeben' }}
            }
          </button>
          <button
            type="button"
            class="btn btn-danger"
            [disabled]="pending"
            (click)="toggleRejectKommentar()"
          >
            Reject
          </button>
          <button
            type="button"
            class="btn btn-secondary"
            [disabled]="pending || reachabilityLaeuft()"
            (click)="oeffneReachabilityDialog()"
            title="Startet eine Reachability-Analyse fuer das zugehoerige Finding."
          >
            @if (reachabilityLaeuft()) {
              <cvm-icon name="loader" [size]="16" class="animate-spin"></cvm-icon>
              Reachability laeuft...
            } @else {
              <cvm-icon name="sparkles" [size]="16"></cvm-icon>
              Reachability starten
            }
          </button>
          @if (showRejectKommentar) {
            <button
              type="button"
              class="btn btn-danger"
              [disabled]="pending || rejectKommentar.trim().length === 0"
              (click)="onReject()"
            >
              Reject bestaetigen
            </button>
          }
        </footer>
      } @else {
        <div class="flex h-full items-center justify-center p-8 text-sm text-text-muted">
          Keinen Vorschlag ausgewaehlt. J/K bewegt durch die Liste.
        </div>
      }
    </section>

    <cvm-dialog
      [open]="reachabilityDialogOffen()"
      title="Reachability-Analyse starten"
      size="lg"
      (close)="abbrechenReachability()"
    >
      @if (entry) {
        <form #reachForm="ngForm" (ngSubmit)="starteReachability()" class="flex flex-col gap-4">
          <p class="text-xs text-text-muted">
            Finding <code class="text-code">{{ entry.findingId }}</code>
            @if (entry.cveKey) {
              <span>&middot; {{ entry.cveKey }}</span>
            }
          </p>

          <label class="form-group">
            <span class="form-label form-label--required">Repository-URL</span>
            <input
              type="text"
              name="repoUrl"
              required
              class="input-field"
              [ngModel]="reachFormular().repoUrl"
              (ngModelChange)="updateReachFeld('repoUrl', $event)"
              placeholder="https://git.example/portalcore.git"
            />
            <span class="form-help">Wird im Read-Only-Modus ausgecheckt.</span>
          </label>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label class="form-group">
              <span class="form-label">Branch (optional)</span>
              <input
                type="text"
                name="branch"
                class="input-field"
                [ngModel]="reachFormular().branch"
                (ngModelChange)="updateReachFeld('branch', $event)"
                placeholder="main"
              />
            </label>
            <label class="form-group">
              <span class="form-label">Commit-SHA (optional)</span>
              <input
                type="text"
                name="commitSha"
                class="input-field"
                [ngModel]="reachFormular().commitSha"
                (ngModelChange)="updateReachFeld('commitSha', $event)"
                placeholder="a3f9beef..."
              />
            </label>
          </div>

          <label class="form-group">
            <span class="form-label form-label--required">Vulnerable Symbol</span>
            <input
              type="text"
              name="vulnerableSymbol"
              required
              class="input-field"
              [ngModel]="reachFormular().vulnerableSymbol"
              (ngModelChange)="updateReachFeld('vulnerableSymbol', $event)"
              placeholder="org.apache.commons.text.StringSubstitutor#replace"
            />
            <span class="form-help">Vollqualifizierter Name der anfaelligen Funktion / Methode.</span>
          </label>

          @if (reachVorschlag(); as v) {
            <div class="banner banner-info" role="status">
              <cvm-icon name="sparkles" [size]="18"></cvm-icon>
              <div class="flex flex-col gap-1">
                @if (v.symbol) {
                  <span>
                    Vorgeschlagen:
                    <button
                      type="button"
                      class="font-mono text-xs px-2 py-0.5 rounded border border-border bg-surface hover:bg-surface-muted"
                      (click)="uebernehmeReachabilityVorschlag()"
                    >
                      {{ v.symbol }}
                    </button>
                  </span>
                } @else {
                  <span>Keine automatische Ableitung moeglich - bitte manuell eingeben.</span>
                }
                @if (v.rationale) {
                  <span class="text-xs text-text-muted">{{ v.rationale }}</span>
                }
              </div>
            </div>
          }

          <label class="form-group">
            <span class="form-label">Sprache</span>
            <select
              name="language"
              class="select-field"
              [ngModel]="reachFormular().language"
              (ngModelChange)="updateReachFeld('language', $event)"
            >
              @for (s of reachSprachen; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
          </label>

          <label class="form-group">
            <span class="form-label">Zusaetzliche Anweisung (optional)</span>
            <textarea
              name="instruction"
              rows="3"
              class="textarea-field"
              [ngModel]="reachFormular().instruction"
              (ngModelChange)="updateReachFeld('instruction', $event)"
              placeholder="z.B. Ignoriere Test-Sources."
            ></textarea>
          </label>

          @if (reachFehler(); as f) {
            <div class="banner banner-critical" role="alert">
              <cvm-icon name="alert-circle" [size]="18"></cvm-icon>
              <span>{{ f }}</span>
            </div>
          }
        </form>
      }
      <div footer>
        <button
          type="button"
          class="btn btn-secondary"
          (click)="abbrechenReachability()"
          [disabled]="reachabilityLaeuft()"
        >
          Abbrechen
        </button>
        <button
          type="button"
          class="btn btn-primary"
          (click)="starteReachability()"
          [disabled]="reachabilityLaeuft() || !reachFormularValid()"
        >
          @if (reachabilityLaeuft()) {
            <cvm-icon name="loader" [size]="16" class="animate-spin"></cvm-icon>
            Analyse laeuft...
          } @else {
            <cvm-icon name="play" [size]="16"></cvm-icon>
            Analyse starten
          }
        </button>
      </div>
    </cvm-dialog>
  `
})
export class QueueDetailComponent implements OnChanges {
  private readonly toast = inject(CvmToastService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly reachabilityService = inject(ReachabilityQueryService);

  @Input() entry: QueueEntry | null = null;
  @Input() pending = false;

  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly approve = new EventEmitter<{
    severity: Severity;
    rationale: string;
    strategy?: string;
    targetVersion?: string;
    plannedFor?: string;
    zweitfreigabe: boolean;
  }>();
  @Output() readonly reject = new EventEmitter<string>();

  readonly severities = SEVERITY_REIHENFOLGE;

  // Muss synchron zum Backend-Enum bleiben:
  // com.ahs.cvm.domain.enums.MitigationStrategy.
  readonly mitigationStrategien = [
    'UPGRADE',
    'PATCH',
    'CONFIG_CHANGE',
    'WORKAROUND',
    'ACCEPT_RISK',
    'NOT_APPLICABLE'
  ] as const;

  zielSeverity: Severity = 'MEDIUM';
  rationale = '';
  strategy = '';
  targetVersion = '';
  plannedFor = '';
  rejectKommentar = '';
  showRejectKommentar = false;

  // Reachability-Dialog (inline, Signal-gesteuert).
  readonly reachSprachen = REACH_SPRACHEN;
  readonly reachabilityDialogOffen = signal(false);
  readonly reachabilityLaeuft = signal(false);
  readonly reachFormular = signal<ReachabilityFormState>(leeresReachabilityFormular());
  readonly reachFehler = signal<string | null>(null);
  readonly reachVorschlag = signal<ReachabilitySuggestion | null>(null);

  ngOnChanges(): void {
    if (this.entry) {
      this.zielSeverity = this.entry.severity;
      this.rationale = this.entry.rationale ?? '';
      this.strategy = '';
      this.targetVersion = '';
      this.plannedFor = '';
      this.rejectKommentar = '';
      this.showRejectKommentar = false;
    }
  }

  get zweitfreigabe(): boolean {
    return braucheZweitfreigabe(this.zielSeverity, this.entry?.severity ?? null);
  }

  /**
   * Iteration 86 (CVM-326): Vier-Augen-Warnung schon vor dem
   * Klick. `entry.decidedBy` ist der Vorschlags-Steller; wer den
   * Vorschlag stellt, darf ihn nicht selbst freigeben.
   */
  get selbstfreigabeKonflikt(): boolean {
    const ich = this.auth.username();
    const autor = this.entry?.decidedBy;
    return !!(ich && autor && ich === autor);
  }

  onApprove(): void {
    this.approve.emit({
      severity: this.zielSeverity,
      rationale: this.rationale,
      strategy: this.strategy || undefined,
      targetVersion: this.targetVersion || undefined,
      plannedFor: this.plannedFor || undefined,
      zweitfreigabe: this.zweitfreigabe
    });
  }

  toggleRejectKommentar(): void {
    this.showRejectKommentar = !this.showRejectKommentar;
  }

  onReject(): void {
    if (this.rejectKommentar.trim().length === 0) {
      return;
    }
    this.reject.emit(this.rejectKommentar.trim());
  }

  oeffneReachabilityDialog(): void {
    const aktuell = this.entry;
    if (!aktuell || this.reachabilityLaeuft()) {
      return;
    }
    const gespeichert = ladeGespeicherteReachability();
    this.reachFormular.set(gespeichert ?? leeresReachabilityFormular());
    this.reachFehler.set(null);
    this.reachVorschlag.set(null);
    this.reachabilityDialogOffen.set(true);
    void this.ladeReachabilityVorschlag(aktuell.findingId);
  }

  abbrechenReachability(): void {
    if (this.reachabilityLaeuft()) {
      return;
    }
    this.reachabilityDialogOffen.set(false);
    this.reachFehler.set(null);
  }

  updateReachFeld<K extends keyof ReachabilityFormState>(
    key: K,
    value: ReachabilityFormState[K]
  ): void {
    this.reachFormular.update((f) => ({ ...f, [key]: value }));
  }

  uebernehmeReachabilityVorschlag(): void {
    const v = this.reachVorschlag();
    if (!v || !v.symbol) {
      return;
    }
    this.reachFormular.update((f) => ({
      ...f,
      vulnerableSymbol: v.symbol!,
      language: v.language || f.language
    }));
  }

  reachFormularValid(): boolean {
    const f = this.reachFormular();
    return f.repoUrl.trim().length > 0 && f.vulnerableSymbol.trim().length > 0;
  }

  async starteReachability(): Promise<void> {
    const aktuell = this.entry;
    if (!aktuell) {
      return;
    }
    if (!this.reachFormularValid()) {
      this.reachFehler.set('Repo-URL und Vulnerable Symbol sind Pflichtfelder.');
      return;
    }
    const state = this.reachFormular();
    const triggeredBy = this.auth.username() || 'anonymous';
    const request: ReachabilityStartRequest = {
      repoUrl: state.repoUrl.trim(),
      branch: state.branch.trim() || null,
      commitSha: state.commitSha.trim() || null,
      vulnerableSymbol: state.vulnerableSymbol.trim(),
      language: state.language || null,
      instruction: state.instruction.trim() || null,
      triggeredBy
    };
    this.reachFehler.set(null);
    this.reachabilityLaeuft.set(true);
    try {
      const result = await this.reachabilityService.startAnalysis(
        aktuell.findingId,
        request
      );
      speichereReachability(state);
      this.reachabilityDialogOffen.set(false);
      this.onReachabilityResult(result);
    } catch (err) {
      const msg =
        err instanceof Error && err.message
          ? err.message
          : 'Reachability-Analyse fehlgeschlagen.';
      this.reachFehler.set(msg);
    } finally {
      this.reachabilityLaeuft.set(false);
    }
  }

  private async ladeReachabilityVorschlag(findingId: string): Promise<void> {
    try {
      const v = await this.reachabilityService.suggestion(findingId);
      this.reachVorschlag.set(v);
      this.reachFormular.update((f) => ({
        ...f,
        vulnerableSymbol: f.vulnerableSymbol || v.symbol || '',
        language: f.language || v.language || f.language
      }));
    } catch {
      // Suggestion-Endpoint nicht verfuegbar oder 404 -> leise
      // ignorieren, UI funktioniert unveraendert.
    }
  }

  private onReachabilityResult(result: ReachabilityResult | null): void {
    if (!result) {
      return;
    }
    if (!result.available) {
      const hinweis =
        result.noteIfUnavailable?.trim()
        || 'Kein Detail vom Backend gemeldet.';
      this.toast.warning('Reachability nicht verfuegbar: ' + hinweis);
      return;
    }
    const kurz =
      result.summary && result.summary.trim().length > 0
        ? result.summary.trim()
        : (result.recommendation ?? 'Analyse abgeschlossen.');
    this.toast.success('Reachability fertig: ' + kurz, 8000);
    // Uebersicht direkt oeffnen; der Toast bleibt lang genug sichtbar.
    void this.router.navigate(['/reachability']);
  }
}
