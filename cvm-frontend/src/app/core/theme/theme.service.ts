import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'cvm.theme';
const DATA_ATTRIBUTE = 'data-theme';

/**
 * Light/Dark-Theme-Umschalter (Iteration 24, CVM-55).
 *
 * <p>Setzt ein {@code data-theme}-Attribut am {@code <html>}-Element.
 * Die Tailwind-Config (darkMode: ['class', '[data-theme="dark"]'])
 * und die CSS-Variablen in {@code styles.scss} reagieren darauf.
 *
 * <p>Der gewaehlte Modus wird im {@code localStorage} unter
 * {@code cvm.theme} persistiert. Beim ersten Besuch greift die
 * System-Praeferenz via {@code matchMedia('(prefers-color-scheme)')}.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storage: Storage | null = this.resolveStorage();

  readonly mode = signal<ThemeMode>(this.resolveInitialMode());

  init(): void {
    this.apply(this.mode());
  }

  set(mode: ThemeMode): void {
    this.mode.set(mode);
    this.apply(mode);
  }

  toggle(): ThemeMode {
    const next: ThemeMode = this.mode() === 'dark' ? 'light' : 'dark';
    this.set(next);
    return next;
  }

  private apply(mode: ThemeMode): void {
    const root = this.document.documentElement;
    if (root) {
      root.setAttribute(DATA_ATTRIBUTE, mode);
    }
    try {
      this.storage?.setItem(STORAGE_KEY, mode);
    } catch {
      // localStorage ggf. verweigert (privater Modus).
    }
  }

  private resolveInitialMode(): ThemeMode {
    const stored = this.readStored();
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
    }
    return 'light';
  }

  private readStored(): string | null {
    try {
      return this.storage?.getItem(STORAGE_KEY) ?? null;
    } catch {
      return null;
    }
  }

  private resolveStorage(): Storage | null {
    try {
      return typeof window !== 'undefined' ? window.localStorage : null;
    } catch {
      return null;
    }
  }
}
