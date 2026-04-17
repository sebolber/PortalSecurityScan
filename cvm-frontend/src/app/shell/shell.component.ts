import { Component, OnInit, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { RoleMenuService } from '../core/auth/role-menu.service';
import { LocaleService } from '../core/i18n/locale.service';

@Component({
  selector: 'cvm-shell',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatSelectModule,
    MatFormFieldModule
  ],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly menu = inject(RoleMenuService);
  private readonly locale = inject(LocaleService);

  readonly texte = this.locale.messages;
  readonly username = this.auth.username;
  readonly loggedIn = this.auth.loggedIn;

  readonly menuEintraege = computed(() =>
    this.menu.visibleEntries(this.auth.userRoles())
  );

  readonly produkte: readonly { key: string; label: string }[] = [
    { key: 'PortalCore-Test', label: 'PortalCore-Test (1.14.2-test)' },
    { key: 'SmileKH-Test', label: 'SmileKH-Test' }
  ];

  selectedProduct = this.produkte[0]?.key ?? '';

  ngOnInit(): void {
    this.auth.refreshFromKeycloak();
  }

  async login(): Promise<void> {
    await this.auth.login();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
