import { Injectable, inject, signal } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

/**
 * Schmaler Wrapper um {@link KeycloakService}. Liefert reaktive Signals
 * fuer Login-Status und Rollen, damit Komponenten mit dem
 * Angular-Signals-API arbeiten koennen.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KeycloakService);

  readonly loggedIn = signal<boolean>(false);
  readonly userRoles = signal<readonly string[]>([]);
  readonly username = signal<string>('');

  refreshFromKeycloak(): void {
    let loggedIn = false;
    try {
      loggedIn = this.keycloak.isLoggedIn();
    } catch {
      loggedIn = false;
    }
    this.loggedIn.set(loggedIn);
    if (!loggedIn) {
      this.userRoles.set([]);
      this.username.set('');
      return;
    }
    // KeycloakService wirft "User not logged in or user profile was not
    // loaded.", wenn das Profil noch nicht eingeladen ist (Default bei
    // check-sso). Wir greifen defensiv und holen das Profil bei Bedarf
    // asynchron nach.
    let roles: string[] = [];
    try {
      roles = this.keycloak.getUserRoles(true);
    } catch {
      roles = [];
    }
    this.userRoles.set(roles);
    try {
      this.username.set(this.keycloak.getUsername() ?? '');
    } catch {
      // UI-Fix HIGH-3 (UI-Exploration 20260418): loadUserProfile()
      // ruft den Keycloak-Account-Endpoint (/realms/<r>/account), der
      // im Default-Realm kein CORS erlaubt. Das hat beim jeden Wechsel
      // in der Konsole Fehler produziert. Da wir username/email nur
      // fuer das Header-Label brauchen und keycloak-angular den
      // Username aus dem ID-Token herauszieht, ist der Account-Call
      // verzichtbar. Wir fallen zurueck auf das Token-Claim
      // `preferred_username`/`email`, wenn getUsername() nicht liefert.
      const tokenParsed = (this.keycloak.getKeycloakInstance?.() as
        | { tokenParsed?: { preferred_username?: string; email?: string } }
        | undefined)?.tokenParsed;
      this.username.set(
        tokenParsed?.preferred_username ?? tokenParsed?.email ?? ''
      );
    }
  }

  async login(): Promise<void> {
    await this.keycloak.login();
  }

  async logout(): Promise<void> {
    await this.keycloak.logout(window.location.origin);
  }

  async getToken(): Promise<string> {
    return this.keycloak.getToken();
  }

  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }
}
