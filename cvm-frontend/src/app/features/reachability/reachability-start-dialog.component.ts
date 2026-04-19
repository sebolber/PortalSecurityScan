import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import {
  ReachabilityQueryService,
  ReachabilityResult,
  ReachabilityStartRequest
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
 * Dialog zum Starten einer Reachability-Analyse. Die Pflichtfelder
 * {@code repoUrl} und {@code vulnerableSymbol} werden per Form-
 * Required erzwungen; {@code triggeredBy} kommt aus dem Auth-Service
 * und wird als Input in die Dialog-Data mitgegeben.
 *
 * <p>Nach erfolgreichem Call wird der Dialog mit dem Ergebnis
 * geschlossen; der Aufrufer entscheidet ueber Snackbar und optionales
 * Refresh der /reachability-Uebersicht.
 */
@Component({
  selector: 'cvm-reachability-start-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  template: `
    <h2 mat-dialog-title>Reachability-Analyse starten</h2>
    <form #formRef="ngForm" (ngSubmit)="starte(formRef)">
      <mat-dialog-content class="cvm-reachability-dialog-content">
        @if (data.cveKey) {
          <p class="cvm-reachability-dialog-sub">
            Finding <code>{{ data.findingId }}</code>
            <span>&middot;</span>
            <span>{{ data.cveKey }}</span>
          </p>
        } @else {
          <p class="cvm-reachability-dialog-sub">
            Finding <code>{{ data.findingId }}</code>
          </p>
        }

        <mat-form-field appearance="outline" class="cvm-reachability-dialog-wide">
          <mat-label>Repository-URL</mat-label>
          <input matInput name="repoUrl" required
                 [ngModel]="formular().repoUrl"
                 (ngModelChange)="updateFeld('repoUrl', $event)"
                 placeholder="https://git.example/portalcore.git">
          <mat-hint>Wird im Read-Only-Modus ausgecheckt.</mat-hint>
        </mat-form-field>

        <div class="cvm-reachability-dialog-row">
          <mat-form-field appearance="outline">
            <mat-label>Branch (optional)</mat-label>
            <input matInput name="branch"
                   [ngModel]="formular().branch"
                   (ngModelChange)="updateFeld('branch', $event)"
                   placeholder="main">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Commit-SHA (optional)</mat-label>
            <input matInput name="commitSha"
                   [ngModel]="formular().commitSha"
                   (ngModelChange)="updateFeld('commitSha', $event)"
                   placeholder="a3f9beef...">
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="cvm-reachability-dialog-wide">
          <mat-label>Vulnerable Symbol</mat-label>
          <input matInput name="vulnerableSymbol" required
                 [ngModel]="formular().vulnerableSymbol"
                 (ngModelChange)="updateFeld('vulnerableSymbol', $event)"
                 placeholder="org.apache.commons.text.StringSubstitutor#replace">
          <mat-hint>Vollqualifizierter Name der anfaelligen Funktion / Methode.</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Sprache</mat-label>
          <mat-select name="language"
                      [ngModel]="formular().language"
                      (ngModelChange)="updateFeld('language', $event)">
            @for (s of sprachen; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="cvm-reachability-dialog-wide">
          <mat-label>Zusaetzliche Anweisung (optional)</mat-label>
          <textarea matInput rows="3" name="instruction"
                    [ngModel]="formular().instruction"
                    (ngModelChange)="updateFeld('instruction', $event)"
                    placeholder="z.B. Ignoriere Test-Sources."></textarea>
        </mat-form-field>

        @if (fehler(); as f) {
          <div class="cvm-reachability-dialog-error" role="alert">
            <mat-icon aria-hidden="true">error_outline</mat-icon>
            <span>{{ f }}</span>
          </div>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="abbrechen()"
                [disabled]="laeuft()">
          Abbrechen
        </button>
        <button mat-raised-button color="primary" type="submit"
                [disabled]="laeuft() || formRef.invalid">
          @if (laeuft()) {
            <mat-spinner diameter="18"></mat-spinner>
          } @else {
            <mat-icon>play_arrow</mat-icon>
          }
          {{ laeuft() ? 'Analyse laeuft...' : 'Analyse starten' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [`
    .cvm-reachability-dialog-content {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      min-width: 520px;
      max-width: 760px;
    }
    .cvm-reachability-dialog-sub {
      margin: 0 0 0.5rem 0;
      color: var(--cvm-text-muted);
      font-size: 0.85rem;
    }
    .cvm-reachability-dialog-wide { width: 100%; }
    .cvm-reachability-dialog-row {
      display: flex;
      gap: 0.5rem;
    }
    .cvm-reachability-dialog-row > mat-form-field { flex: 1 1 0; }
    .cvm-reachability-dialog-error {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.6rem 0.75rem;
      border-radius: 6px;
      background: color-mix(in srgb, #d32f2f 10%, transparent);
      color: #d32f2f;
      border: 1px solid color-mix(in srgb, #d32f2f 40%, transparent);
      font-size: 0.85rem;
    }
  `]
})
export class ReachabilityStartDialogComponent implements OnInit {
  private readonly service = inject(ReachabilityQueryService);
  private readonly dialogRef = inject(
    MatDialogRef<ReachabilityStartDialogComponent, ReachabilityResult | null>
  );

  readonly sprachen = SPRACHEN;
  readonly formular = signal<FormState>(leeresFormular());
  readonly laeuft = signal(false);
  readonly fehler = signal<string | null>(null);

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: ReachabilityStartDialogInput
  ) {}

  ngOnInit(): void {
    const gespeichert = ladeGespeicherteFormularwerte();
    if (gespeichert) {
      this.formular.set(gespeichert);
    }
  }

  updateFeld<K extends keyof FormState>(key: K, value: FormState[K]): void {
    this.formular.update((f) => ({ ...f, [key]: value }));
  }

  async starte(form: NgForm): Promise<void> {
    if (form.invalid) {
      this.fehler.set('Repo-URL und Vulnerable Symbol sind Pflichtfelder.');
      return;
    }
    const aktuell = this.formular();
    const request: ReachabilityStartRequest = {
      repoUrl: aktuell.repoUrl.trim(),
      branch: aktuell.branch.trim() || null,
      commitSha: aktuell.commitSha.trim() || null,
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
      this.dialogRef.close(result);
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
    this.dialogRef.close(null);
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
    // Secret-freie Felder sind das; trotzdem speichern wir nur die
    // Repo-/Branch-Kombination, weil Symbol und Instruction pro
    // Finding variieren und beim naechsten Mal leer starten sollen.
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      repoUrl: state.repoUrl,
      branch: state.branch,
      commitSha: state.commitSha,
      language: state.language
    }));
  } catch {
    // Private-Mode o.ae. - stumm verworfen, Feature funktioniert trotzdem.
  }
}
