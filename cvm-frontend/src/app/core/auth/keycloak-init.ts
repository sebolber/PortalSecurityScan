import { KeycloakService } from 'keycloak-angular';
import { AppConfigService } from '../config/app-config.service';

/**
 * Initialisiert Keycloak vor App-Bootstrap. Liest die Config aus
 * {@link AppConfigService} (welche bereits ueber einen vorgelagerten
 * APP_INITIALIZER geladen wurde) und initialisiert
 * {@link KeycloakService} mit {@code check-sso} (kein Hard-Redirect,
 * damit ein nicht erreichbarer Keycloak die App nicht blockiert).
 *
 * <p><strong>Defensiv:</strong> Schlaegt {@code keycloak.init(...)} fehl
 * (KC nicht erreichbar, Realm/Client falsch konfiguriert,
 * Iframe-Mechanismus blockiert), so wird der Fehler ins Console-Log
 * geschrieben und {@code true} zurueckgegeben. Damit blockiert eine
 * fehlerhafte Auth-Konfiguration nicht den App-Bootstrap; die Shell
 * rendert auch ohne Login, der AuthGuard triggert den Login bei
 * Bedarf neu, sobald eine geschuetzte Route aufgerufen wird.
 */
export function initializeKeycloak(
  keycloak: KeycloakService,
  config: AppConfigService
): () => Promise<boolean> {
  return async () => {
    const cfg = config.get();
    try {
      return await keycloak.init({
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
    } catch (err) {
      // Bewusst kein throw: App soll auch ohne Keycloak starten, damit
      // der Benutzer die Shell sieht und einen Retry-Button hat.
      console.error('Keycloak-Init fehlgeschlagen, App startet ohne Login:', err);
      return false;
    }
  };
}
