import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { QueueApiService } from './queue-api.service';
import {
  ApproveCommand,
  QueueEntry,
  QueueFilter,
  RejectCommand,
  SEVERITY_RANK
} from './queue.types';
import { Severity } from '../../shared/components/severity-badge.component';

/**
 * Leichtgewichtiger Signal-Store fuer die Bewertungs-Queue.
 *
 * <p>Quelle der Wahrheit ist {@link QueueApiService}. Der Store haelt
 * geladene Eintraege im Speicher, verwaltet Filter und Auswahl und
 * aktualisiert die UI optimistisch bei approve/reject. Schlaegt ein
 * Backend-Call fehl, wird der zuvor entfernte Eintrag wieder eingefuegt.
 */
@Injectable({ providedIn: 'root' })
export class QueueStore {
  private readonly api = inject(QueueApiService);

  private readonly entriesSig = signal<readonly QueueEntry[]>([]);
  private readonly filterSig = signal<QueueFilter>({});
  private readonly selectedIdSig = signal<string | null>(null);
  private readonly loadingSig = signal<boolean>(false);
  private readonly pendingSig = signal<ReadonlySet<string>>(new Set());
  private readonly errorSig = signal<string | null>(null);
  private readonly checkedIdsSig = signal<ReadonlySet<string>>(new Set());

  readonly filter = this.filterSig.asReadonly();
  readonly loading = this.loadingSig.asReadonly();
  readonly error = this.errorSig.asReadonly();
  readonly selectedId = this.selectedIdSig.asReadonly();
  readonly pending = this.pendingSig.asReadonly();
  readonly checkedIds = this.checkedIdsSig.asReadonly();

  /**
   * Nach Severity sortiert (CRITICAL zuerst), innerhalb gleicher
   * Severity nach {@code createdAt} aufsteigend (FIFO).
   */
  readonly entries = computed<readonly QueueEntry[]>(() => {
    const severityFilter = this.filterSig().severityIn;
    const base = this.entriesSig();
    const gefiltert = severityFilter && severityFilter.length > 0
      ? base.filter((e) => severityFilter.includes(e.severity))
      : base;
    return [...gefiltert].sort((a, b) => {
      const diff = SEVERITY_RANK[b.severity] - SEVERITY_RANK[a.severity];
      if (diff !== 0) {
        return diff;
      }
      return a.createdAt.localeCompare(b.createdAt);
    });
  });

  readonly selected = computed<QueueEntry | null>(() => {
    const id = this.selectedIdSig();
    if (!id) {
      return null;
    }
    return this.entriesSig().find((e) => e.id === id) ?? null;
  });

  readonly checkedCount = computed<number>(() => this.checkedIdsSig().size);

  setFilter(patch: Partial<QueueFilter>): void {
    this.filterSig.update((current) => ({ ...current, ...patch }));
  }

  resetFilter(): void {
    this.filterSig.set({});
  }

  toggleSeverityFilter(severity: Severity): void {
    const current = this.filterSig().severityIn ?? [];
    const next = current.includes(severity)
      ? current.filter((s) => s !== severity)
      : [...current, severity];
    this.filterSig.update((f) => ({ ...f, severityIn: next }));
  }

  select(id: string | null): void {
    this.selectedIdSig.set(id);
  }

  moveSelection(delta: 1 | -1): void {
    const list = this.entries();
    if (list.length === 0) {
      this.selectedIdSig.set(null);
      return;
    }
    const currentId = this.selectedIdSig();
    if (!currentId) {
      this.selectedIdSig.set(list[0].id);
      return;
    }
    const idx = list.findIndex((e) => e.id === currentId);
    if (idx === -1) {
      this.selectedIdSig.set(list[0].id);
      return;
    }
    const target = Math.max(0, Math.min(list.length - 1, idx + delta));
    this.selectedIdSig.set(list[target].id);
  }

  toggleChecked(id: string): void {
    this.checkedIdsSig.update((set) => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  clearChecked(): void {
    this.checkedIdsSig.set(new Set());
  }

  async reload(): Promise<void> {
    this.loadingSig.set(true);
    this.errorSig.set(null);
    try {
      const serverFilter = { ...this.filterSig(), severityIn: undefined };
      const list = await firstValueFrom(this.api.list(serverFilter));
      this.entriesSig.set(list);
      const currentId = this.selectedIdSig();
      if (currentId && !list.some((e) => e.id === currentId)) {
        this.selectedIdSig.set(null);
      }
    } catch (err) {
      this.errorSig.set(this.formatError(err));
    } finally {
      this.loadingSig.set(false);
    }
  }

  async approve(id: string, command: ApproveCommand): Promise<boolean> {
    const snapshot = this.entriesSig();
    const entry = snapshot.find((e) => e.id === id);
    if (!entry) {
      return false;
    }
    this.startPending(id);
    // Optimistisches Entfernen: das Assessment geht in den neuen Status
    // und taucht aus der offenen Queue heraus.
    this.entriesSig.set(snapshot.filter((e) => e.id !== id));
    this.dropChecked(id);
    try {
      await firstValueFrom(this.api.approve(id, command));
      this.unselectIfMatches(id);
      return true;
    } catch (err) {
      this.entriesSig.set(snapshot);
      this.errorSig.set(this.formatError(err));
      return false;
    } finally {
      this.stopPending(id);
    }
  }

  async reject(id: string, command: RejectCommand): Promise<boolean> {
    const snapshot = this.entriesSig();
    const entry = snapshot.find((e) => e.id === id);
    if (!entry) {
      return false;
    }
    this.startPending(id);
    this.entriesSig.set(snapshot.filter((e) => e.id !== id));
    this.dropChecked(id);
    try {
      await firstValueFrom(this.api.reject(id, command));
      this.unselectIfMatches(id);
      return true;
    } catch (err) {
      this.entriesSig.set(snapshot);
      this.errorSig.set(this.formatError(err));
      return false;
    } finally {
      this.stopPending(id);
    }
  }

  isPending(id: string): boolean {
    return this.pendingSig().has(id);
  }

  /** Testhilfe: deterministische Initialbestueckung ohne HTTP. */
  seed(entries: readonly QueueEntry[]): void {
    this.entriesSig.set(entries);
  }

  private startPending(id: string): void {
    this.pendingSig.update((set) => {
      const next = new Set(set);
      next.add(id);
      return next;
    });
  }

  private stopPending(id: string): void {
    this.pendingSig.update((set) => {
      const next = new Set(set);
      next.delete(id);
      return next;
    });
  }

  private dropChecked(id: string): void {
    this.checkedIdsSig.update((set) => {
      if (!set.has(id)) {
        return set;
      }
      const next = new Set(set);
      next.delete(id);
      return next;
    });
  }

  private unselectIfMatches(id: string): void {
    if (this.selectedIdSig() === id) {
      this.selectedIdSig.set(null);
    }
  }

  private formatError(err: unknown): string {
    if (err instanceof Error) {
      return err.message || 'Backend-Aktion fehlgeschlagen.';
    }
    return 'Backend-Aktion fehlgeschlagen.';
  }
}
