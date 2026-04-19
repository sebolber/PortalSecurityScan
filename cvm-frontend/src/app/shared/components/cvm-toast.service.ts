import {
  ApplicationRef,
  ComponentRef,
  EnvironmentInjector,
  Injectable,
  Type,
  createComponent,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, Input, HostBinding } from '@angular/core';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 61A (CVM-62): Ersatz fuer `MatSnackBar`. Zeigt eine
 * Toast-Queue oben rechts. Standard: Auto-Hide nach 4 s fuer
 * `info`/`success`, manuell fuer `warning`/`critical`.
 */
export type CvmToastKind = 'info' | 'success' | 'warning' | 'critical';

export interface CvmToast {
  id: number;
  text: string;
  actionLabel?: string;
  kind: CvmToastKind;
  durationMs: number;
}

@Component({
  selector: 'cvm-toast-host',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="cvm-toast-host" role="region" aria-live="polite">
      @for (t of toasts(); track t.id) {
        <div class="banner banner-{{ kindClass(t.kind) }} cvm-toast" role="status">
          <cvm-icon [name]="iconFor(t.kind)" [size]="18"></cvm-icon>
          <span class="grow text-sm">{{ t.text }}</span>
          <button type="button" class="btn-icon" (click)="dismiss(t.id)" aria-label="Schliessen">
            <cvm-icon name="close" [size]="16"></cvm-icon>
          </button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .cvm-toast-host {
        position: fixed;
        top: 16px;
        right: 16px;
        z-index: 60;
        display: flex;
        flex-direction: column;
        gap: 8px;
        max-width: min(90vw, 420px);
      }
      .cvm-toast {
        box-shadow: var(--shadow-md);
      }
    `
  ]
})
export class CvmToastHostComponent {
  readonly toasts = signal<CvmToast[]>([]);

  push(toast: CvmToast): void {
    this.toasts.update((list) => [...list, toast]);
    if (toast.durationMs > 0) {
      setTimeout(() => this.dismiss(toast.id), toast.durationMs);
    }
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  kindClass(k: CvmToastKind): string {
    return k === 'critical' ? 'critical' : k === 'warning' ? 'warning' : k === 'success' ? 'success' : 'info';
  }

  iconFor(k: CvmToastKind): string {
    return k === 'critical'
      ? 'alert-circle'
      : k === 'warning'
        ? 'alert-triangle'
        : k === 'success'
          ? 'check-circle'
          : 'info';
  }
}

@Injectable({ providedIn: 'root' })
export class CvmToastService {
  private host?: CvmToastHostComponent;
  private nextId = 1;
  private readonly appRef = inject(ApplicationRef);
  private readonly envInjector = inject(EnvironmentInjector);

  private ensureHost(): CvmToastHostComponent {
    if (this.host) return this.host;
    const ref: ComponentRef<CvmToastHostComponent> = createComponent(
      CvmToastHostComponent as Type<CvmToastHostComponent>,
      { environmentInjector: this.envInjector }
    );
    this.appRef.attachView(ref.hostView);
    document.body.appendChild(ref.location.nativeElement);
    this.host = ref.instance;
    return this.host;
  }

  show(text: string, kind: CvmToastKind = 'info', durationMs?: number): void {
    const defaultDuration = kind === 'critical' || kind === 'warning' ? 0 : 4000;
    this.ensureHost().push({
      id: this.nextId++,
      text,
      kind,
      durationMs: durationMs ?? defaultDuration
    });
  }

  info(text: string, durationMs?: number): void {
    this.show(text, 'info', durationMs);
  }

  success(text: string, durationMs?: number): void {
    this.show(text, 'success', durationMs);
  }

  warning(text: string): void {
    this.show(text, 'warning', 0);
  }

  error(text: string): void {
    this.show(text, 'critical', 0);
  }
}
