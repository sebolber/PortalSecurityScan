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
    this.loggedIn.set(this.keycloak.isLoggedIn());
    if (this.loggedIn()) {
      this.userRoles.set(this.keycloak.getUserRoles(true));
      this.username.set(this.keycloak.getUsername() ?? '');
    } else {
      this.userRoles.set([]);
      this.username.set('');
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
