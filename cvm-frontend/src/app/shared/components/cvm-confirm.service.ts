import {
  ApplicationRef,
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  EnvironmentInjector,
  Injectable,
  Type,
  createComponent,
  inject,
  signal
} from '@angular/core';
import { CvmDialogComponent } from './cvm-dialog.component';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 90 (CVM-330): Einheitlicher Bestaetigungs-Dialog als
 * Ersatz fuer `window.confirm`. Wird aus Admin-Flows (Produkte,
 * Versionen, Regeln, Umgebungen, Profile) aufgerufen. Liefert ein
 * Promise<boolean>; bei Abbruch/ESC/Overlay-Klick = `false`.
 */
export type CvmConfirmVariant = 'neutral' | 'danger';

export interface CvmConfirmOptions {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  readonly variant?: CvmConfirmVariant;
}

interface InternalState extends CvmConfirmOptions {
  readonly resolve: (value: boolean) => void;
}

@Component({
  selector: 'cvm-confirm-host',
  standalone: true,
  imports: [CvmDialogComponent, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cvm-dialog
      [open]="!!current()"
      [title]="current()?.title ?? ''"
      size="sm"
      (close)="abbrechen()"
    >
      <p class="text-text-primary whitespace-pre-line" data-testid="cvm-confirm-message">
        {{ current()?.message }}
      </p>

      <div footer class="flex items-center justify-end gap-2">
        <button class="btn btn-secondary" type="button"
                data-testid="cvm-confirm-cancel"
                (click)="abbrechen()">
          {{ current()?.cancelLabel ?? 'Abbrechen' }}
        </button>
        <button type="button"
                [class.btn]="true"
                [class.btn-primary]="current()?.variant !== 'danger'"
                [class.btn-danger]="current()?.variant === 'danger'"
                data-testid="cvm-confirm-ok"
                (click)="bestaetigen()">
          <cvm-icon [name]="current()?.variant === 'danger' ? 'delete' : 'check'" [size]="16"></cvm-icon>
          {{ current()?.confirmLabel ?? 'Bestaetigen' }}
        </button>
      </div>
    </cvm-dialog>
  `
})
export class CvmConfirmHostComponent {
  readonly current = signal<InternalState | null>(null);

  open(options: CvmConfirmOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.current.set({ ...options, resolve });
    });
  }

  bestaetigen(): void {
    this.closeWith(true);
  }

  abbrechen(): void {
    this.closeWith(false);
  }

  private closeWith(value: boolean): void {
    const state = this.current();
    if (!state) return;
    this.current.set(null);
    state.resolve(value);
  }
}

@Injectable({ providedIn: 'root' })
export class CvmConfirmService {
  private host?: CvmConfirmHostComponent;
  private readonly appRef = inject(ApplicationRef);
  private readonly envInjector = inject(EnvironmentInjector);

  confirm(options: CvmConfirmOptions): Promise<boolean> {
    return this.ensureHost().open(options);
  }

  private ensureHost(): CvmConfirmHostComponent {
    if (this.host) return this.host;
    const ref: ComponentRef<CvmConfirmHostComponent> = createComponent(
      CvmConfirmHostComponent as Type<CvmConfirmHostComponent>,
      { environmentInjector: this.envInjector }
    );
    this.appRef.attachView(ref.hostView);
    document.body.appendChild(ref.location.nativeElement);
    this.host = ref.instance;
    return this.host;
  }
}
