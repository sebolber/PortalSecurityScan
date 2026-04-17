import { KeycloakService } from 'keycloak-angular';
import { AppConfigService } from '../config/app-config.service';

/**
 * Initialisiert Keycloak vor App-Bootstrap. Liest die Config aus
 * {@link AppConfigService} (welche bereits ueber einen vorgelagerten
 * APP_INITIALIZER geladen wurde) und initialisiert
 * {@link KeycloakService} mit `check-sso` (kein Hard-Redirect, falls
 * der Backend-Smoke-Test ohne Keycloak laufen soll).
 *
 * Iteration 07 verzichtet auf einen blockierenden Login-Redirect, weil
 * sonst die Karma-Tests gegen Keycloak laufen wuerden. Der AuthGuard
 * triggert die Login-Maske erst beim Aufruf einer geschuetzten Route.
 */
export function initializeKeycloak(
  keycloak: KeycloakService,
  config: AppConfigService
): () => Promise<boolean> {
  return async () => {
    const cfg = config.get();
    return keycloak.init({
      config: {
        url: cfg.keycloak.url,
        realm: cfg.keycloak.realm,
        clientId: cfg.keycloak.clientId
      },
      initOptions: {
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      },
      enableBearerInterceptor: false,
      bearerExcludedUrls: ['/assets']
    });
  };
}
