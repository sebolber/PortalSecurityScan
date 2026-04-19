import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import {
  DryRunResponse,
  RuleResponse,
  RulesService
} from '../../core/rules/rules.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';

interface RuleDraftForm {
  ruleKey: string;
  name: string;
  description: string;
  proposedSeverity: string;
  conditionJson: string;
  rationaleTemplate: string;
  rationaleSourceFields: string;
  origin: string;
}

const SEVERITIES = [
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
  'INFORMATIONAL',
  'NOT_APPLICABLE'
] as const;

const DEFAULT_CONDITION = `{
  "allOf": [
    { "field": "cve.kevListed", "op": "eq", "value": true }
  ]
}`;

function initialDraft(): RuleDraftForm {
  return {
    ruleKey: '',
    name: '',
    description: '',
    proposedSeverity: 'HIGH',
    conditionJson: DEFAULT_CONDITION,
    rationaleTemplate: '',
    rationaleSourceFields: '',
    origin: 'MANUAL'
  };
}

@Component({
  selector: 'cvm-rules',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AhsBannerComponent
  ],
  templateUrl: './rules.component.html',
  styleUrls: ['./rules.component.scss']
})
export class RulesComponent implements OnInit {
  private readonly rulesService = inject(RulesService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly severities = SEVERITIES;

  readonly rules = signal<readonly RuleResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly editorOpen = signal<boolean>(false);
  readonly draft = signal<RuleDraftForm>(initialDraft());
  readonly creating = signal<boolean>(false);
  readonly createError = signal<string | null>(null);

  /** Letztes Dry-Run-Ergebnis pro Rule-ID. */
  readonly dryRuns = signal<Record<string, DryRunResponse | undefined>>({});
  readonly pending = signal<Record<string, boolean>>({});

  readonly canActivate = computed(() => {
    const roles = new Set(this.auth.userRoles());
    return roles.has(CVM_ROLES.RULE_APPROVER) || roles.has(CVM_ROLES.ADMIN);
  });

  readonly canAuthor = computed(() => {
    const roles = new Set(this.auth.userRoles());
    return (
      roles.has(CVM_ROLES.RULE_AUTHOR) ||
      roles.has(CVM_ROLES.RULE_APPROVER) ||
      roles.has(CVM_ROLES.ADMIN)
    );
  });

  /** Iteration 50 (CVM-100): Soft-Delete nur fuer Admin. */
  readonly canAdmin = computed(() =>
    new Set(this.auth.userRoles()).has(CVM_ROLES.ADMIN)
  );

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const list = await this.rulesService.list();
      this.rules.set(list);
    } catch {
      this.error.set('Regeln konnten nicht geladen werden.');
    } finally {
      this.loading.set(false);
    }
  }

  async aktivieren(rule: RuleResponse): Promise<void> {
    const approverId = this.auth.username() || '';
    if (!approverId) {
      this.snackBar.open(
        'Kein Benutzer angemeldet. Aktivieren nicht moeglich.', 'OK',
        { duration: 3000 });
      return;
    }
    if (rule.createdBy === approverId) {
      this.snackBar.open(
        'Vier-Augen: der Anleger darf nicht gleichzeitig aktivieren.', 'OK',
        { duration: 4000 });
      return;
    }
    this.setPending(rule.id, true);
    try {
      await this.rulesService.activate(rule.id, approverId);
      this.snackBar.open(`Regel "${rule.ruleKey}" aktiviert.`, 'OK',
        { duration: 3000 });
      await this.laden();
    } catch {
      // ApiClient zeigt Snackbar.
    } finally {
      this.setPending(rule.id, false);
    }
  }

  /** Iteration 50 (CVM-100): Soft-Delete. */
  async loesche(rule: RuleResponse): Promise<void> {
    const bestaetigt = window.confirm(
      'Regel "' + rule.ruleKey + '" wirklich soft-loeschen?\n\n'
        + 'Soft-Delete = technisch entfernt (Regel-Engine ignoriert sie).\n'
        + 'Unterscheidet sich von RETIRED (fachlich abgeloest). '
        + 'Historische Assessments behalten ihre Rationale.'
    );
    if (!bestaetigt) {
      return;
    }
    this.setPending(rule.id, true);
    try {
      await this.rulesService.delete(rule.id);
      this.snackBar.open('Regel "' + rule.ruleKey + '" entfernt.', 'OK',
        { duration: 3000 });
      await this.laden();
    } catch {
      // ApiClient zeigt Snackbar.
    } finally {
      this.setPending(rule.id, false);
    }
  }

  async dryRun(rule: RuleResponse): Promise<void> {
    this.setPending(rule.id, true);
    try {
      const result = await this.rulesService.dryRun(rule.id, 180);
      const runs = { ...this.dryRuns() };
      runs[rule.id] = result;
      this.dryRuns.set(runs);
    } catch {
      // ApiClient zeigt Snackbar.
    } finally {
      this.setPending(rule.id, false);
    }
  }

  isPending(ruleId: string): boolean {
    return !!this.pending()[ruleId];
  }

  editorUmschalten(): void {
    this.editorOpen.update((v) => !v);
    if (!this.editorOpen()) {
      this.createError.set(null);
    }
  }

  updateDraft<K extends keyof RuleDraftForm>(key: K, value: RuleDraftForm[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  async draftAnlegen(): Promise<void> {
    if (!this.canAuthor()) {
      this.createError.set('Rolle CVM_RULE_AUTHOR erforderlich.');
      return;
    }
    const autor = this.auth.username() || 'unbekannt';
    const form = this.draft();
    if (!form.ruleKey.trim() || !form.name.trim()) {
      this.createError.set('ruleKey und name sind Pflichtfelder.');
      return;
    }
    try {
      JSON.parse(form.conditionJson);
    } catch {
      this.createError.set('conditionJson ist kein gueltiges JSON.');
      return;
    }
    const fields = form.rationaleSourceFields
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    this.creating.set(true);
    this.createError.set(null);
    try {
      const rule = await this.rulesService.create({
        ruleKey: form.ruleKey.trim(),
        name: form.name.trim(),
        description: form.description.trim() || null,
        proposedSeverity: form.proposedSeverity,
        conditionJson: form.conditionJson,
        rationaleTemplate: form.rationaleTemplate.trim() || null,
        rationaleSourceFields: fields,
        origin: form.origin.trim() || null,
        createdBy: autor
      });
      this.snackBar.open(
        'Regel "' + rule.ruleKey + '" angelegt (Status DRAFT).',
        'OK',
        { duration: 4000 }
      );
      this.draft.set(initialDraft());
      this.editorOpen.set(false);
      await this.laden();
    } catch (err) {
      this.createError.set(
        err instanceof Error && err.message
          ? err.message
          : 'Anlegen fehlgeschlagen (Schema-Validierung?).'
      );
    } finally {
      this.creating.set(false);
    }
  }

  private setPending(ruleId: string, value: boolean): void {
    const p = { ...this.pending() };
    p[ruleId] = value;
    this.pending.set(p);
  }
}
