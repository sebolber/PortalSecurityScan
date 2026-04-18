import {
  ChangeDetectionStrategy,
  Component,
  Input,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Zeigt eine UUID als kompakten Chip mit den ersten 8 Zeichen plus
 * einem Copy-Button. Beim Klick auf den Button wird die volle UUID
 * in die Zwischenablage kopiert und ein Snackbar bestaetigt das.
 *
 * <p>Motivation: Skripte und externe Tools (z.B. SBOM-Upload an
 * {@code POST /api/v1/scans}) brauchen die vollstaendige UUID.
 * Die UI zeigte sie bisher nirgends, Admins mussten mit dem
 * Backend-API sprechen.
 */
@Component({
  selector: 'cvm-uuid-chip',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="cvm-uuid-chip">
      <code
        class="cvm-uuid-chip__short"
        [matTooltip]="value"
        matTooltipPosition="above"
      >{{ kurz() }}</code>
      <button
        type="button"
        mat-icon-button
        class="cvm-uuid-chip__copy"
        [attr.aria-label]="'UUID ' + value + ' kopieren'"
        [matTooltip]="kopiert() ? 'Kopiert' : 'In Zwischenablage kopieren'"
        (click)="kopiere($event)"
      >
        <mat-icon fontIcon="content_copy" class="cvm-uuid-chip__icon"
          >{{ kopiert() ? 'check' : 'content_copy' }}</mat-icon
        >
      </button>
    </span>
  `,
  styles: [
    `
      .cvm-uuid-chip {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        font-family: var(--font-family-mono);
      }
      .cvm-uuid-chip__short {
        font-size: 0.8rem;
        padding: 0.1rem 0.4rem;
        border-radius: var(--radius-sm, 4px);
        background: var(--color-surface-muted, #f1f1f1);
        color: var(--color-text, #222);
        cursor: help;
      }
      .cvm-uuid-chip__copy {
        width: 28px;
        height: 28px;
        line-height: 28px;
      }
      .cvm-uuid-chip__icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    `
  ]
})
export class UuidChipComponent {
  private readonly snack = inject(MatSnackBar);
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
        // Fallback fuer Kontexte ohne Clipboard-API (z.B. unsicherer
        // Kontext in IE-Edge-Alt, Test-Sandbox).
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
      this.snack.open('UUID kopiert', 'OK', { duration: 2000 });
      // Icon nach kurzem Delay zuruecksetzen.
      setTimeout(() => this.kopiertSig.set(false), 2000);
    } catch {
      this.snack.open(
        'Kopieren fehlgeschlagen - UUID bitte manuell markieren.',
        'OK',
        { duration: 4000 }
      );
    }
  }
}
