import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { AppConfigService } from '../config/app-config.service';

/**
 * HTTP-Interceptor:
 * <ul>
 *   <li>Setzt {@code Authorization: Bearer <token>} fuer Requests an die
 *       konfigurierte API-Base-URL.</li>
 *   <li>Bei 401 wird der KC-Logout angestossen, damit ein abgelaufener
 *       Token nicht stille Folgefehler nach sich zieht.</li>
 * </ul>
 *
 * Lokale Asset-Calls (z.&nbsp;B. {@code assets/config.json}) werden nicht
 * angefasst.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const config = inject(AppConfigService);

  let apiBase: string | null;
  try {
    apiBase = config.get().apiBaseUrl;
  } catch {
    apiBase = null;
  }
  const istApiCall = apiBase !== null && req.url.startsWith(apiBase);
  if (!istApiCall) {
    return next(req);
  }

  return from(auth.getToken()).pipe(
    switchMap((token) => {
      const authed = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;
      return next(authed).pipe(
        catchError((err) => {
          if (err?.status === 401) {
            void auth.logout();
          }
          return throwError(() => err);
        })
      );
    })
  );
};
