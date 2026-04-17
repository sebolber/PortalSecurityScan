import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { RoleMenuService } from './role-menu.service';

/**
 * Schuetzt App-Routen. Greift auf `data['requiredRoles']` der Route zu;
 * fehlt der Eintrag, wird die Route ueber {@link RoleMenuService}
 * automatisch geprueft (Mapping path -> Rollen).
 *
 * Bei fehlendem Login wird der Keycloak-Login getriggert.
 */
export const authGuard: CanActivateFn = async (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const menu = inject(RoleMenuService);

  auth.refreshFromKeycloak();
  if (!auth.loggedIn()) {
    await auth.login();
    return false;
  }

  const requiredRoles =
    (route.data?.['requiredRoles'] as readonly string[] | undefined) ?? null;
  if (requiredRoles && requiredRoles.length > 0) {
    return requiredRoles.some((r) => auth.hasRole(r))
      || router.createUrlTree(['/dashboard']);
  }

  if (menu.hasAccess(state.url, auth.userRoles())) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
