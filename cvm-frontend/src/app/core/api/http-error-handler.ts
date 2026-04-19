import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

/**
 * Zentraler Mapping-Layer fuer HTTP-Fehler. Zeigt eine deutschsprachige
 * Toast-Meldung. Ersetzt `MatSnackBar` (Iteration 61A, CVM-62).
 */
@Injectable({ providedIn: 'root' })
export class HttpErrorHandler {
  private readonly toast = inject(CvmToastService);

  show(context: string, error: HttpErrorResponse): void {
    this.toast.error(this.format(context, error));
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
