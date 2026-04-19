import {
  Directive,
  EventEmitter,
  HostListener,
  Output,
  inject
} from '@angular/core';
import { Router } from '@angular/router';

/**
 * Iteration 91 (CVM-331): Globale Tastatur-Shortcuts. Haengt am
 * Shell-Root. Shortcuts:
 *  - `?`         oeffnet den globalen Shortcut-Sheet
 *  - `g` `d`     navigiert zu /dashboard
 *  - `g` `q`     navigiert zu /queue
 *  - `g` `s`     navigiert zu /scans/upload
 *  - `g` `w`     navigiert zu /waivers
 *  - `g` `r`     navigiert zu /reports
 *
 * Shortcuts werden **nicht** ausgeloest, wenn der Fokus in einem
 * Editier-Element liegt (INPUT/TEXTAREA/SELECT/contenteditable).
 */
@Directive({
  selector: '[cvmGlobalShortcuts]',
  standalone: true
})
export class GlobalShortcutsDirective {
  @Output() readonly help = new EventEmitter<void>();

  private readonly router = inject(Router);

  /** Letzter Key, gesetzt beim ersten 'g' fuer 2-Tasten-Kombinationen. */
  private prefix: 'g' | null = null;
  private prefixTimer: ReturnType<typeof setTimeout> | null = null;

  @HostListener('document:keydown', ['$event'])
  handleKey(event: KeyboardEvent): void {
    if (this.shouldIgnore(event)) {
      this.resetPrefix();
      return;
    }

    if (this.prefix === 'g') {
      this.resetPrefix();
      const target = this.targetFor(event.key);
      if (target) {
        event.preventDefault();
        void this.router.navigateByUrl(target);
      }
      return;
    }

    if (event.key === '?' && !event.shiftKey && event.code !== 'Slash') {
      event.preventDefault();
      this.help.emit();
      return;
    }
    if (event.key === '?' || (event.key === '/' && event.shiftKey)) {
      event.preventDefault();
      this.help.emit();
      return;
    }
    if (event.key === 'g') {
      event.preventDefault();
      this.prefix = 'g';
      this.prefixTimer = setTimeout(() => this.resetPrefix(), 1500);
      return;
    }
  }

  private targetFor(key: string): string | null {
    switch (key) {
      case 'd':
        return '/dashboard';
      case 'q':
        return '/queue';
      case 's':
        return '/scans/upload';
      case 'w':
        return '/waivers';
      case 'r':
        return '/reports';
      default:
        return null;
    }
  }

  private resetPrefix(): void {
    this.prefix = null;
    if (this.prefixTimer) {
      clearTimeout(this.prefixTimer);
      this.prefixTimer = null;
    }
  }

  private shouldIgnore(event: KeyboardEvent): boolean {
    if (event.ctrlKey || event.metaKey || event.altKey) {
      return true;
    }
    const target = event.target as HTMLElement | null;
    if (!target) {
      return false;
    }
    if (target.isContentEditable) {
      return true;
    }
    const tag = target.tagName;
    return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
  }
}
