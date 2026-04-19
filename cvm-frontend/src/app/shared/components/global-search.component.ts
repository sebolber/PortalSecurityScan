import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { MenuEntry, RoleMenuService } from '../../core/auth/role-menu.service';
import { CvmDialogComponent } from './cvm-dialog.component';
import { CvmIconComponent } from './cvm-icon.component';

export interface GlobalSearchHit {
  readonly label: string;
  readonly path: string;
  readonly kind: 'menu' | 'cve';
  readonly hint?: string;
}

const CVE_PATTERN = /^CVE-\d{4}-\d{4,7}$/i;

/**
 * Iteration 92 (CVM-332): Globaler Such-Dialog. Nimmt den
 * Eingabetext, filtert sichtbare Menue-Eintraege per Substring und
 * bietet - wenn der Text einer CVE-ID entspricht - einen direkten
 * Deep-Link zur CVE-Detail-Seite. Enter navigiert zum ersten
 * Treffer; Klick auf eine Zeile navigiert ebenfalls.
 */
@Component({
  selector: 'cvm-global-search',
  standalone: true,
  imports: [CommonModule, FormsModule, CvmDialogComponent, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="visible"
      title="Suche"
      size="md"
      (close)="close.emit()"
    >
      <div class="flex flex-col gap-3" data-testid="global-search-body">
        <label class="form-group">
          <span class="form-label">Seite oder CVE-ID</span>
          <input
            class="input-field"
            type="text"
            autocomplete="off"
            data-testid="global-search-input"
            [ngModel]="query()"
            (ngModelChange)="setQuery($event)"
            (keyup.enter)="waehleErsten()"
            placeholder="z.B. Queue, Profile, CVE-2017-18640"
          />
        </label>

        @if (hits().length === 0) {
          <p class="text-sm text-text-muted" data-testid="global-search-empty">
            Keine Treffer. Probiere einen Menue-Namen oder eine vollstaendige
            CVE-ID.
          </p>
        } @else {
          <ul class="flex flex-col divide-y divide-border rounded-md border border-border"
              data-testid="global-search-results">
            @for (hit of hits(); track hit.path; let i = $index) {
              <li>
                <button
                  type="button"
                  class="w-full flex items-center gap-2 px-3 py-2 text-left hover:bg-surface-muted"
                  [attr.data-testid]="'global-search-hit-' + i"
                  (click)="waehle(hit)"
                >
                  <cvm-icon
                    [name]="hit.kind === 'cve' ? 'shield' : 'arrow-right'"
                    [size]="16"
                    class="text-text-muted"
                  ></cvm-icon>
                  <span class="grow font-medium">{{ hit.label }}</span>
                  @if (hit.hint) {
                    <span class="text-xs text-text-muted">{{ hit.hint }}</span>
                  }
                </button>
              </li>
            }
          </ul>
        }
      </div>

      <div footer class="flex items-center justify-between">
        <span class="text-xs text-text-muted">Enter = ersten Treffer oeffnen</span>
        <button type="button" class="btn btn-secondary" (click)="close.emit()">
          Abbrechen
        </button>
      </div>
    </cvm-dialog>
  `
})
export class GlobalSearchComponent {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();

  private readonly router = inject(Router);
  private readonly menu = inject(RoleMenuService);
  private readonly auth = inject(AuthService);

  readonly query = signal<string>('');

  readonly hits = computed<readonly GlobalSearchHit[]>(() =>
    this.computeHits(this.query().trim())
  );

  setQuery(value: string): void {
    this.query.set(value ?? '');
  }

  waehle(hit: GlobalSearchHit): void {
    void this.router.navigateByUrl(hit.path);
    this.query.set('');
    this.close.emit();
  }

  waehleErsten(): void {
    const first = this.hits()[0];
    if (first) {
      this.waehle(first);
    }
  }

  private computeHits(q: string): readonly GlobalSearchHit[] {
    if (!q) return [];
    const hits: GlobalSearchHit[] = [];
    if (CVE_PATTERN.test(q)) {
      const id = q.toUpperCase();
      hits.push({
        label: id,
        path: `/cves/${id}`,
        kind: 'cve',
        hint: 'CVE-Detail oeffnen'
      });
    }
    const roles = this.auth.userRoles();
    const normalized = q.toLowerCase();
    const menuHits = this.menu
      .visibleEntries(roles)
      .flatMap((e) => this.flatten(e))
      .filter((e) => e.label.toLowerCase().includes(normalized))
      .map<GlobalSearchHit>((e) => ({
        label: e.label,
        path: e.path,
        kind: 'menu'
      }));
    hits.push(...menuHits);
    return hits;
  }

  private flatten(entry: MenuEntry): MenuEntry[] {
    const out: MenuEntry[] = [entry];
    for (const c of entry.children ?? []) {
      out.push(...this.flatten(c));
    }
    return out;
  }
}
