import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { AppConfigService } from '../config/app-config.service';
import { HttpErrorHandler } from './http-error-handler';

/**
 * Schmaler HTTP-Wrapper, der die API-Base-URL aus der
 * {@link AppConfigService} ableitet und Fehler ueber den
 * {@link HttpErrorHandler} (Snackbar) zentral meldet.
 *
 * Komponenten benutzen die {@code get/post/put}-Methoden, anstatt
 * {@link HttpClient} direkt einzubinden &mdash; so bleibt die URL-Logik
 * an einer Stelle.
 */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);
  private readonly errorHandler = inject(HttpErrorHandler);

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(this.url(path)).pipe(this.handleError('GET ' + path));
  }

  post<T, B = unknown>(path: string, body: B): Observable<T> {
    return this.http.post<T>(this.url(path), body).pipe(this.handleError('POST ' + path));
  }

  put<T, B = unknown>(path: string, body: B): Observable<T> {
    return this.http.put<T>(this.url(path), body).pipe(this.handleError('PUT ' + path));
  }

  delete<T = void>(path: string): Observable<T> {
    return this.http
      .delete<T>(this.url(path))
      .pipe(this.handleError('DELETE ' + path));
  }

  url(path: string): string {
    const base = this.config.get().apiBaseUrl.replace(/\/$/, '');
    const suffix = path.startsWith('/') ? path : '/' + path;
    return base + suffix;
  }

  private handleError<T>(label: string) {
    return catchError<T, Observable<T>>((err: HttpErrorResponse) => {
      this.errorHandler.show(label, err);
      return throwError(() => err);
    });
  }
}
