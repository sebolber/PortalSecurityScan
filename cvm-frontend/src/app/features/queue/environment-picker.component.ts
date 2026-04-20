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
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

/**
 * Iteration 98 (CVM-340): Suchdialog fuer Umgebungen. Suche
 * ueber key/name/stage.
 */
@Component({
  selector: 'cvm-environment-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, CvmDialogComponent, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="visible"
      title="Umgebung waehlen"
      size="md"
      (close)="close.emit()"
    >
      <div class="flex flex-col gap-3" data-testid="environment-picker">
        <label class="form-group">
          <span class="form-label">Suche</span>
          <input
            class="input-field"
            type="text"
            data-testid="environment-picker-search"
            [ngModel]="suche()"
            (ngModelChange)="suche.set($event ?? '')"
            placeholder="REF-TEST, PROD, ..."
            autocomplete="off"
          />
        </label>

        @if (laedt()) {
          <p class="text-caption flex items-center gap-2">
            <cvm-icon name="loader" [size]="14" class="animate-spin"></cvm-icon>
            Laedt Umgebungen...
          </p>
        } @else if (fehler()) {
          <p class="form-error" data-testid="environment-picker-error">
            {{ fehler() }}
          </p>
        } @else if (treffer().length === 0) {
          <p class="text-caption" data-testid="environment-picker-empty">
            Keine Treffer.
          </p>
        } @else {
          <ul
            class="flex flex-col divide-y divide-border border border-border rounded-md max-h-80 overflow-auto"
            data-testid="environment-picker-list"
          >
            @for (e of treffer(); track e.id) {
              <li>
                <button
                  type="button"
                  class="w-full text-left px-3 py-2 hover:bg-surface-muted flex items-center gap-3"
                  [attr.data-testid]="'environment-pick-' + e.id"
                  (click)="waehle(e)"
                >
                  <span class="inline-flex flex-col min-w-0 grow">
                    <span class="font-medium truncate">{{ e.name }}</span>
                    <span class="text-xs text-text-muted truncate">
                      {{ e.key }} &middot; {{ e.stage }}
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
export class EnvironmentPickerComponent implements OnInit {
  @Input() visible = false;
  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly selected = new EventEmitter<{
    environmentId: string;
    label: string;
  }>();

  private readonly service = inject(EnvironmentsService);

  readonly suche = signal<string>('');
  readonly laedt = signal<boolean>(false);
  readonly fehler = signal<string | null>(null);
  readonly eintraege = signal<readonly EnvironmentView[]>([]);

  readonly treffer = computed<readonly EnvironmentView[]>(() => {
    const q = this.suche().trim().toLowerCase();
    if (!q) return this.eintraege();
    return this.eintraege().filter((e) =>
      e.key.toLowerCase().includes(q) ||
      e.name.toLowerCase().includes(q) ||
      e.stage.toLowerCase().includes(q)
    );
  });

  async ngOnInit(): Promise<void> {
    this.laedt.set(true);
    this.fehler.set(null);
    try {
      this.eintraege.set(await this.service.list());
    } catch {
      this.fehler.set('Umgebungen konnten nicht geladen werden.');
      this.eintraege.set([]);
    } finally {
      this.laedt.set(false);
    }
  }

  waehle(env: EnvironmentView): void {
    this.selected.emit({
      environmentId: env.id,
      label: `${env.name} (${env.stage})`
    });
  }
}
