import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
  signal
} from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import {
  ReachabilityQueryService,
  ReachabilityResult,
  ReachabilityStartContext,
  ReachabilityStartRequest,
  ReachabilitySuggestion
} from '../../core/reachability/reachability.service';

export interface ReachabilityStartDialogInput {
  readonly findingId: string;
  readonly cveKey?: string;
  readonly triggeredBy: string;
}

interface FormState {
  repoUrl: string;
  branch: string;
  commitSha: string;
  vulnerableSymbol: string;
  language: string;
  instruction: string;
}

const STORAGE_KEY = 'cvm.reachability.last';
const SPRACHEN = [
  'java',
  'javascript',
  'typescript',
  'python',
  'go',
  'rust'
] as const;

function leeresFormular(): FormState {
  return {
    repoUrl: '',
    branch: '',
    commitSha: '',
    vulnerableSymbol: '',
    language: 'java',
    instruction: ''
  };
}

/**
 * Dialog zum Starten einer Reachability-Analyse.
 *
 * Iteration 61 (CVM-62): Refactor von MatDialogRef/MAT_DIALOG_DATA auf
 * reine Standalone-Component mit {@code @Input}/{@code @Output}. Der
 * Dialog wird vom Host inline ueber {@code <cvm-dialog>} gerendert und
 * per {@code [open]}-Signal gesteuert.
 */
@Component({
  selector: 'cvm-reachability-start-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CvmDialogComponent,
    CvmIconComponent
  ],
  template: `
    <cvm-dialog
      [open]="open"
      title="Reachability-Analyse starten"
      size="lg"
      [closeOnOverlay]="!laeuft()"
      (close)="abbrechen()"
    >
      @if (open && data) {
        <form #formRef="ngForm" (ngSubmit)="starte(formRef)" class="flex flex-col gap-4">
          <p class="text-sm text-text-muted">
            Finding <code class="text-code">{{ data.findingId }}</code>
            @if (data.cveKey) {
              <span> &middot; {{ data.cveKey }}</span>
            }
          </p>

          @if (kontext()?.rationale; as r) {
            <div class="banner banner-info" role="status" data-testid="reachability-context-hint">
              <cvm-icon name="info" [size]="16"></cvm-icon>
              <span class="text-sm">{{ r }}</span>
            </div>
          }

          <label class="form-group">
            <span class="form-label form-label--required">Repository-URL</span>
            <input
              type="text"
              class="input-field"
              name="repoUrl"
              required
              data-testid="reachability-repo-url"
              [ngModel]="formular().repoUrl"
              (ngModelChange)="updateFeld('repoUrl', $event)"
              placeholder="https://git.example/portalcore.git"
            />
            <span class="form-help">Wird im Read-Only-Modus ausgecheckt.</span>
          </label>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label class="form-group">
              <span class="form-label">Branch (optional)</span>
              <input
                type="text"
                class="input-field"
                name="branch"
                data-testid="reachability-branch"
                [ngModel]="formular().branch"
                (ngModelChange)="updateFeld('branch', $event)"
                placeholder="main"
              />
            </label>

            <label class="form-group">
              <span class="form-label form-label--required">Commit-SHA</span>
              <input
                type="text"
                class="input-field"
                name="commitSha"
                required
                data-testid="reachability-commit"
                [ngModel]="formular().commitSha"
                (ngModelChange)="updateFeld('commitSha', $event)"
                placeholder="a3f9beef..."
              />
              <span class="form-help">Pflicht - JGit clont genau diesen Commit.</span>
            </label>
          </div>

          <label class="form-group">
            <span class="form-label form-label--required">Vulnerable Symbol</span>
            <input
              type="text"
              class="input-field"
              name="vulnerableSymbol"
              required
              data-testid="reachability-symbol"
              [ngModel]="formular().vulnerableSymbol"
              (ngModelChange)="updateFeld('vulnerableSymbol', $event)"
              placeholder="org.apache.commons.text.StringSubstitutor#replace"
            />
            <span class="form-help">Vollqualifizierter Name der anfaelligen Funktion / Methode.</span>
          </label>

          @if (vorschlag(); as v) {
            <div class="banner banner-info" role="status">
              <cvm-icon name="sparkles" [size]="18"></cvm-icon>
              <div class="flex flex-col gap-1 grow">
                @if (v.symbol) {
                  <span class="text-sm">
                    Vorgeschlagen:
                    <button
                      type="button"
                      class="text-code px-2 py-0.5 rounded border border-border bg-surface hover:bg-surface-muted"
                      (click)="uebernehmeVorschlag()"
                    >{{ v.symbol }}</button>
                  </span>
                } @else {
                  <span class="text-sm">Keine automatische Ableitung moeglich - bitte manuell eingeben.</span>
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
              class="select-field"
              name="language"
              data-testid="reachability-language"
              [ngModel]="formular().language"
              (ngModelChange)="updateFeld('language', $event)"
            >
              @for (s of sprachen; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
          </label>

          <label class="form-group">
            <span class="form-label">Zusaetzliche Anweisung (optional)</span>
            <textarea
              class="textarea-field"
              name="instruction"
              rows="3"
              data-testid="reachability-instruction"
              [ngModel]="formular().instruction"
              (ngModelChange)="updateFeld('instruction', $event)"
              placeholder="z.B. Ignoriere Test-Sources."
            ></textarea>
          </label>

          @if (fehler(); as f) {
            <div class="banner banner-critical" role="alert">
              <cvm-icon name="alert-circle" [size]="18"></cvm-icon>
              <span>{{ f }}</span>
            </div>
          }

          <!-- hidden submit so Enter-Taste im Formular den Submit ausloest -->
          <button type="submit" class="hidden" aria-hidden="true"></button>
        </form>
      }

      <div footer>
        <button
          type="button"
          class="btn btn-secondary"
          data-testid="reachability-cancel"
          [disabled]="laeuft()"
          (click)="abbrechen()"
        >Abbrechen</button>
        <button
          type="button"
          class="btn btn-primary"
          data-testid="reachability-submit"
          [disabled]="laeuft() || !istFormularGueltig()"
          (click)="starteDirekt()"
        >
          @if (laeuft()) {
            <cvm-icon name="loader" [size]="16" class="animate-spin"></cvm-icon>
          } @else {
            <cvm-icon name="play" [size]="16"></cvm-icon>
          }
          {{ laeuft() ? 'Analyse laeuft...' : 'Analyse starten' }}
        </button>
      </div>
    </cvm-dialog>
  `
})
export class ReachabilityStartDialogComponent implements OnInit, OnChanges {
  private readonly service = inject(ReachabilityQueryService);

  @Input() open = false;
  @Input() data: ReachabilityStartDialogInput | null = null;

  @Output() readonly confirm = new EventEmitter<ReachabilityResult>();
  @Output() readonly cancel = new EventEmitter<void>();

  readonly sprachen = SPRACHEN;
  readonly formular = signal<FormState>(leeresFormular());
  readonly laeuft = signal(false);
  readonly fehler = signal<string | null>(null);
  readonly vorschlag = signal<ReachabilitySuggestion | null>(null);
  // Iteration 97 (CVM-339): Kontext-Vorbelegung (Repo + Commit).
  readonly kontext = signal<ReachabilityStartContext | null>(null);

  ngOnInit(): void {
    this.ladeGespeichert();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && changes['open'].currentValue === true) {
      // Bei jedem Oeffnen: Zustand zuruecksetzen und Vorschlag laden.
      this.fehler.set(null);
      this.vorschlag.set(null);
      this.kontext.set(null);
      this.ladeGespeichert();
      if (this.data) {
        void this.ladeVorschlag();
        void this.ladeKontext();
      }
    }
  }

  /**
   * Iteration 97 (CVM-339): Holt Repo-URL und Commit-SHA aus
   * Produkt/Produkt-Version und befuellt die leeren Felder vor.
   * Bestehende Nutzereingaben werden nicht ueberschrieben.
   */
  private async ladeKontext(): Promise<void> {
    if (!this.data) return;
    try {
      const k = await this.service.context(this.data.findingId);
      this.kontext.set(k);
      this.formular.update((f) => ({
        ...f,
        repoUrl: f.repoUrl || k.repoUrl || '',
        commitSha: f.commitSha || k.commitSha || ''
      }));
    } catch {
      // Context-Endpoint optional - leise ignorieren, User kann
      // die Felder manuell befuellen.
    }
  }

  private ladeGespeichert(): void {
    const gespeichert = ladeGespeicherteFormularwerte();
    if (gespeichert) {
      this.formular.set(gespeichert);
    } else {
      this.formular.set(leeresFormular());
    }
  }

  private async ladeVorschlag(): Promise<void> {
    if (!this.data) return;
    try {
      const v = await this.service.suggestion(this.data.findingId);
      this.vorschlag.set(v);
      this.formular.update((f) => ({
        ...f,
        vulnerableSymbol: f.vulnerableSymbol || v.symbol || '',
        language: f.language || v.language || f.language
      }));
    } catch {
      // Suggestion-Endpoint nicht verfuegbar oder 404 -> leise ignorieren.
    }
  }

  uebernehmeVorschlag(): void {
    const v = this.vorschlag();
    if (!v || !v.symbol) {
      return;
    }
    this.formular.update((f) => ({
      ...f,
      vulnerableSymbol: v.symbol!,
      language: v.language || f.language
    }));
  }

  updateFeld<K extends keyof FormState>(key: K, value: FormState[K]): void {
    this.formular.update((f) => ({ ...f, [key]: value }));
  }

  istFormularGueltig(): boolean {
    const f = this.formular();
    return (
      f.repoUrl.trim().length > 0 &&
      f.commitSha.trim().length > 0 &&
      f.vulnerableSymbol.trim().length > 0
    );
  }

  async starte(form: NgForm): Promise<void> {
    if (form.invalid) {
      this.fehler.set(
        'Repo-URL, Commit-SHA und Vulnerable Symbol sind Pflichtfelder.'
      );
      return;
    }
    await this.starteDirekt();
  }

  async starteDirekt(): Promise<void> {
    if (!this.data) {
      return;
    }
    if (!this.istFormularGueltig()) {
      this.fehler.set(
        'Repo-URL, Commit-SHA und Vulnerable Symbol sind Pflichtfelder.'
      );
      return;
    }
    const aktuell = this.formular();
    const request: ReachabilityStartRequest = {
      repoUrl: aktuell.repoUrl.trim(),
      branch: aktuell.branch.trim() || null,
      commitSha: aktuell.commitSha.trim(),
      vulnerableSymbol: aktuell.vulnerableSymbol.trim(),
      language: aktuell.language || null,
      instruction: aktuell.instruction.trim() || null,
      triggeredBy: this.data.triggeredBy
    };
    this.fehler.set(null);
    this.laeuft.set(true);
    try {
      const result = await this.service.startAnalysis(
        this.data.findingId, request);
      speichereFormularwerte(aktuell);
      this.confirm.emit(result);
    } catch (err) {
      const msg = err instanceof Error && err.message
        ? err.message
        : 'Reachability-Analyse fehlgeschlagen.';
      this.fehler.set(msg);
    } finally {
      this.laeuft.set(false);
    }
  }

  abbrechen(): void {
    if (this.laeuft()) {
      return;
    }
    this.cancel.emit();
  }
}

function ladeGespeicherteFormularwerte(): FormState | null {
  if (typeof localStorage === 'undefined') {
    return null;
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<FormState>;
    return {
      ...leeresFormular(),
      ...parsed,
      // vulnerableSymbol und instruction sind findingsspezifisch,
      // deshalb nicht aus dem Cache einblenden.
      vulnerableSymbol: '',
      instruction: ''
    };
  } catch {
    return null;
  }
}

function speichereFormularwerte(state: FormState): void {
  if (typeof localStorage === 'undefined') {
    return;
  }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      repoUrl: state.repoUrl,
      branch: state.branch,
      commitSha: state.commitSha,
      language: state.language
    }));
  } catch {
    // Private-Mode o.ae. - stumm verworfen.
  }
}
