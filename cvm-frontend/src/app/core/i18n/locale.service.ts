import { Injectable, signal } from '@angular/core';
import { MESSAGES_DE, Messages } from './messages.de';

const STORAGE_KEY = 'cvm.locale';
const SUPPORTED = ['de', 'en'] as const;
export type Locale = (typeof SUPPORTED)[number];

/**
 * Sehr schmaler Locale-Service. Default ist {@code de}, andere Sprachen
 * folgen, sobald die Texte uebersetzt sind. Texte werden synchron
 * aufgeloest; ein Wechsel zwischen Sprachen ist vorbereitet, aber noch
 * nicht aktiv.
 */
@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly current = signal<Locale>(this.detect());

  readonly locale = this.current.asReadonly();
  readonly messages: Messages = MESSAGES_DE;

  setLocale(locale: Locale): void {
    if (!SUPPORTED.includes(locale)) {
      return;
    }
    this.current.set(locale);
    try {
      window.localStorage.setItem(STORAGE_KEY, locale);
    } catch {
      // Storage z.B. im Inkognito-Modus blockiert; ignoriert.
    }
  }

  private detect(): Locale {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      if (stored && (SUPPORTED as readonly string[]).includes(stored)) {
        return stored as Locale;
      }
    } catch {
      // ignore
    }
    return 'de';
  }
}
