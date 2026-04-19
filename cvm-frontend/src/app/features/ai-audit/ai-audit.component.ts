import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AhsCardComponent } from '../../shared/components/ahs-card.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import {
  AiAuditService,
  AiCallAuditView,
  AiCallStatus
} from '../../core/aiaudit/ai-audit.service';

/**
 * AI-Audit-Sicht (Iteration 11 Nachzug).
 *
 * <p>Liest die ai_call_audit-Tabelle paginiert ueber
 * {@code GET /api/v1/ai/audits}. Nur Metadaten - System-/User-Prompts
 * sind aus Datenschutzgruenden NICHT exponiert.
 */
@Component({
  selector: 'cvm-ai-audit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AhsCardComponent,
    CvmIconComponent,
    CvmDialogComponent,
    EmptyStateComponent
  ],
  templateUrl: './ai-audit.component.html',
  styleUrls: ['./ai-audit.component.scss']
})
export class AiAuditComponent implements OnInit {
  private readonly api = inject(AiAuditService);

  readonly statusOptions: readonly (AiCallStatus | 'ALL')[] = [
    'ALL', 'PENDING', 'OK', 'INVALID_OUTPUT', 'INJECTION_RISK',
    'ERROR', 'RATE_LIMITED', 'DISABLED'
  ];

  statusFilter: AiCallStatus | 'ALL' = 'ALL';
  useCaseFilter = '';

  readonly entries = signal<readonly AiCallAuditView[]>([]);
  readonly totalElements = signal<number>(0);
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(20);
  readonly busy = signal<boolean>(false);
  readonly fehler = signal<string | null>(null);

  // Iteration 61 (CVM-62): Details-Dialog statt MatDialog.
  readonly selected = signal<AiCallAuditView | null>(null);

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async applyFilter(): Promise<void> {
    this.pageIndex.set(0);
    await this.refresh();
  }

  async prevPage(): Promise<void> {
    if (this.pageIndex() === 0) return;
    this.pageIndex.update((i) => i - 1);
    await this.refresh();
  }

  async nextPage(): Promise<void> {
    if ((this.pageIndex() + 1) * this.pageSize() >= this.totalElements()) return;
    this.pageIndex.update((i) => i + 1);
    await this.refresh();
  }

  async changePageSize(size: number): Promise<void> {
    this.pageSize.set(size);
    this.pageIndex.set(0);
    await this.refresh();
  }

  oeffneDetails(entry: AiCallAuditView): void {
    this.selected.set(entry);
  }

  schliesseDetails(): void {
    this.selected.set(null);
  }

  isStatusOk(s: AiCallStatus): boolean {
    return s === 'OK';
  }

  isStatusWarn(s: AiCallStatus): boolean {
    return s === 'PENDING';
  }

  isStatusErr(s: AiCallStatus): boolean {
    return s !== 'OK' && s !== 'PENDING';
  }

  private async refresh(): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.fehler.set(null);
    try {
      const page = await firstValueFrom(this.api.liste({
        status: this.statusFilter === 'ALL' ? undefined : this.statusFilter,
        useCase: this.useCaseFilter.trim() || undefined,
        page: this.pageIndex(),
        size: this.pageSize()
      }));
      this.entries.set(page.content);
      this.totalElements.set(page.totalElements);
    } catch (err) {
      this.fehler.set(this.formatError(err));
      this.entries.set([]);
      this.totalElements.set(0);
    } finally {
      this.busy.set(false);
    }
  }

  private formatError(err: unknown): string {
    if (err && typeof err === 'object') {
      const obj = err as Record<string, unknown>;
      const status = typeof obj['status'] === 'number' ? obj['status'] : null;
      if (status === 401 || status === 403) {
        return 'Keine Berechtigung. AI_AUDITOR oder CVM_ADMIN erforderlich.';
      }
    }
    return 'Konnte AI-Audit-Liste nicht laden.';
  }
}
