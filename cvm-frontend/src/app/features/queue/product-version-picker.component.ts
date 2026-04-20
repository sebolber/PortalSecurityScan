import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

interface Zeile {
  readonly versionId: string;
  readonly produktKey: string;
  readonly produktName: string;
  readonly version: string;
  readonly gitCommit: string | null;
}

/**
 * Iteration 98 (CVM-340): Suchdialog fuer Produkt-Versionen.
 * Listet Produkte mit allen Versionen und liefert die ausgewaehlte
 * Version-UUID per Output zurueck. Suchfeld filtert ueber
 * Produkt-Name, Produkt-Key und Versionstext.
 */
@Component({
  selector: 'cvm-product-version-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, CvmDialogComponent, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="visible"
      title="Produkt-Version waehlen"
      size="lg"
      (close)="close.emit()"
    >
      <div class="flex flex-col gap-3" data-testid="product-version-picker">
        <label class="form-group">
          <span class="form-label">Suche</span>
          <input
            class="input-field"
            type="text"
            data-testid="product-version-picker-search"
            [ngModel]="suche()"
            (ngModelChange)="suche.set($event ?? '')"
            placeholder="Produkt oder Version..."
            autocomplete="off"
          />
        </label>

        @if (laedt()) {
          <p class="text-caption flex items-center gap-2">
            <cvm-icon name="loader" [size]="14" class="animate-spin"></cvm-icon>
            Laedt Produkte und Versionen...
          </p>
        } @else if (fehler()) {
          <p class="form-error" data-testid="product-version-picker-error">
            {{ fehler() }}
          </p>
        } @else if (treffer().length === 0) {
          <p class="text-caption" data-testid="product-version-picker-empty">
            Keine Treffer.
          </p>
        } @else {
          <ul
            class="flex flex-col divide-y divide-border border border-border rounded-md max-h-80 overflow-auto"
            data-testid="product-version-picker-list"
          >
            @for (z of treffer(); track z.versionId) {
              <li>
                <button
                  type="button"
                  class="w-full text-left px-3 py-2 hover:bg-surface-muted flex items-center gap-3"
                  [attr.data-testid]="'product-version-pick-' + z.versionId"
                  (click)="waehle(z.versionId)"
                >
                  <span class="inline-flex flex-col min-w-0 grow">
                    <span class="font-medium truncate">
                      {{ z.produktName }} &middot; {{ z.version }}
                    </span>
                    <span class="text-xs text-text-muted truncate">
                      {{ z.produktKey }}@if (z.gitCommit) {
                        &middot; <code>{{ z.gitCommit.slice(0, 7) }}</code>
                      }
                    </span>
                  </span>
                  <cvm-icon name="arrow-right" [size]="16" class="text-text-muted"></cvm-icon>
                </button>
              </li>
            }
          </ul>
        }
      </div>

      <div footer class="flex items-center justify-end">
        <button type="button" class="btn btn-secondary" (click)="close.emit()">
          Abbrechen
        </button>
      </div>
    </cvm-dialog>
  `
})
export class ProductVersionPickerComponent implements OnInit {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly selected = new EventEmitter<{
    versionId: string;
    label: string;
  }>();

  private readonly products = inject(ProductsService);

  readonly suche = signal<string>('');
  readonly laedt = signal<boolean>(false);
  readonly fehler = signal<string | null>(null);
  readonly zeilen = signal<readonly Zeile[]>([]);

  readonly treffer = computed<readonly Zeile[]>(() => {
    const q = this.suche().trim().toLowerCase();
    if (!q) return this.zeilen();
    return this.zeilen().filter((z) =>
      z.produktKey.toLowerCase().includes(q) ||
      z.produktName.toLowerCase().includes(q) ||
      z.version.toLowerCase().includes(q)
    );
  });

  async ngOnInit(): Promise<void> {
    this.laedt.set(true);
    this.fehler.set(null);
    try {
      const produkte = await this.products.list();
      const alle = await this.versionen(produkte);
      this.zeilen.set(alle);
    } catch {
      this.fehler.set(
        'Produkte und Versionen konnten nicht geladen werden.'
      );
      this.zeilen.set([]);
    } finally {
      this.laedt.set(false);
    }
  }

  waehle(versionId: string): void {
    const z = this.zeilen().find((x) => x.versionId === versionId);
    if (!z) return;
    this.selected.emit({
      versionId,
      label: `${z.produktName} ${z.version}`
    });
  }

  private async versionen(
    produkte: readonly ProductView[]
  ): Promise<readonly Zeile[]> {
    const alle: Zeile[] = [];
    for (const p of produkte) {
      let vs: readonly ProductVersionView[] = [];
      try {
        vs = await this.products.versions(p.id);
      } catch {
        continue;
      }
      for (const v of vs) {
        alle.push({
          versionId: v.id,
          produktKey: p.key,
          produktName: p.name,
          version: v.version,
          gitCommit: v.gitCommit
        });
      }
    }
    return alle;
  }
}
