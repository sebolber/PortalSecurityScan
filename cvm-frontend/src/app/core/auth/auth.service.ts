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
      this.username.set('');
      void this.keycloak.loadUserProfile()
        .then((profile) =>
          this.username.set(profile?.username ?? profile?.email ?? ''))
        .catch(() => undefined);
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
