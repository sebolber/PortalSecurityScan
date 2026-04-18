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
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import {
  DryRunResponse,
  RuleResponse,
  RulesService
} from '../../core/rules/rules.service';

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
    MatInputModule
  ],
  templateUrl: './rules.component.html',
  styleUrls: ['./rules.component.scss']
})
export class RulesComponent implements OnInit {
  private readonly rulesService = inject(RulesService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly rules = signal<readonly RuleResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

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

  private setPending(ruleId: string, value: boolean): void {
    const p = { ...this.pending() };
    p[ruleId] = value;
    this.pending.set(p);
  }
}
