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
 *   <li>Bei 401 wird der KC-Logout NUR dann angestossen, wenn der User
 *       zuvor eingeloggt war (abgelaufener Token). Wenn er gar nicht
 *       eingeloggt ist, ist 401 erwartetes Verhalten und darf keinen
 *       Logout-Redirect triggern - sonst entsteht eine Reload-Loop
 *       (Shell -> Banner-Polling -> 401 -> Logout-Redirect -> Shell).</li>
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

  // Wenn nicht eingeloggt, gar nicht erst auf einen Token warten -
  // sonst hat KeycloakService.getToken() unter check-sso ein
  // Promise, das nie resolvet und zu haengenden Requests fuehrt.
  if (!auth.loggedIn()) {
    return next(req).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  return from(auth.getToken()).pipe(
    switchMap((token) => {
      const authed = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;
      return next(authed).pipe(
        catchError((err) => {
          if (err?.status === 401 && auth.loggedIn()) {
            // Token war da, ist aber abgelaufen -> Logout.
            void auth.logout();
          }
          return throwError(() => err);
        })
      );
    })
  );
};
