import {
  ChangeDetectionStrategy,
  Component,
  Input,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CvmIconComponent } from './cvm-icon.component';
import { CvmToastService } from './cvm-toast.service';

/**
 * Zeigt eine UUID als kompakten Chip mit den ersten 8 Zeichen plus
 * einem Copy-Button. Iteration 61A: Pure Tailwind, lucide-Icons,
 * CvmToastService statt MatSnackBar.
 */
@Component({
  selector: 'cvm-uuid-chip',
  standalone: true,
  imports: [CommonModule, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex items-center gap-1 font-mono">
      <code
        class="rounded-sm bg-surface-muted px-1.5 py-0.5 text-xs text-text"
        [attr.title]="value"
      >{{ kurz() }}</code>
      <button
        type="button"
        class="btn-icon !w-7 !h-7"
        [attr.aria-label]="'UUID ' + value + ' kopieren'"
        [attr.title]="kopiert() ? 'Kopiert' : 'In Zwischenablage kopieren'"
        (click)="kopiere($event)"
      >
        <cvm-icon [name]="kopiert() ? 'check' : 'content-copy'" [size]="14"></cvm-icon>
      </button>
    </span>
  `
})
export class UuidChipComponent {
  private readonly toast = inject(CvmToastService);
  private readonly kopiertSig = signal(false);

  @Input({ required: true }) value = '';

  readonly kopiert = this.kopiertSig.asReadonly();
  readonly kurz = computed(() => {
    const v = this.value ?? '';
    if (v.length <= 10) {
      return v;
    }
    return v.slice(0, 8) + '…';
  });

  async kopiere(event: MouseEvent): Promise<void> {
    event.stopPropagation();
    if (!this.value) {
      return;
    }
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(this.value);
      } else {
        const ta = document.createElement('textarea');
        ta.value = this.value;
        ta.setAttribute('readonly', '');
        ta.style.position = 'absolute';
        ta.style.left = '-9999px';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        ta.remove();
      }
      this.kopiertSig.set(true);
      this.toast.success('UUID kopiert', 2000);
      setTimeout(() => this.kopiertSig.set(false), 2000);
    } catch {
      this.toast.error('Kopieren fehlgeschlagen - UUID bitte manuell markieren.');
    }
  }
}
