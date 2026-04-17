import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Zentraler Mapping-Layer fuer HTTP-Fehler. Zeigt eine deutschsprachige
 * Snackbar an. Gemeinsam genutzt vom {@link ApiClient} und ad-hoc-
 * Aufrufen aus Feature-Services.
 */
@Injectable({ providedIn: 'root' })
export class HttpErrorHandler {
  private readonly snackBar = inject(MatSnackBar);

  show(context: string, error: HttpErrorResponse): void {
    const meldung = this.format(context, error);
    this.snackBar.open(meldung, 'Schliessen', {
      duration: 6000,
      panelClass: ['cvm-snack-error']
    });
  }

  format(context: string, error: HttpErrorResponse): string {
    const status = error?.status ?? 0;
    if (status === 0) {
      return `${context}: Backend nicht erreichbar.`;
    }
    if (status >= 500) {
      return `${context}: Serverfehler (${status}). Bitte spaeter erneut versuchen.`;
    }
    if (status === 401) {
      return `${context}: Nicht angemeldet (401).`;
    }
    if (status === 403) {
      return `${context}: Keine Berechtigung (403).`;
    }
    if (status === 404) {
      return `${context}: Nicht gefunden (404).`;
    }
    if (status === 409) {
      return `${context}: Konflikt (409): ${this.detail(error)}`;
    }
    if (status >= 400) {
      return `${context}: Eingabefehler (${status}): ${this.detail(error)}`;
    }
    return `${context}: Unbekannter Fehler (${status}).`;
  }

  private detail(error: HttpErrorResponse): string {
    const body = error?.error;
    if (typeof body === 'string') {
      return body;
    }
    if (body && typeof body === 'object') {
      const obj = body as Record<string, unknown>;
      const msg = obj['message'] ?? obj['error'];
      if (typeof msg === 'string') {
        return msg;
      }
    }
    return error.message ?? 'unbekannte Ursache';
  }
}
