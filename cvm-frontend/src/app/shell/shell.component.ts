import { Component, DestroyRef, OnInit, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { RoleMenuService } from '../core/auth/role-menu.service';
import { CVM_ROLE_LABELS, CvmRole } from '../core/auth/cvm-roles';
import { LocaleService } from '../core/i18n/locale.service';
import { AlertBannerService } from '../core/alerts/alert-banner.service';
import { ThemeService } from '../core/theme/theme.service';
import { AlertBannerComponent } from './alert-banner.component';

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
    MatFormFieldModule,
    MatTooltipModule,
    AlertBannerComponent
  ],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly menu = inject(RoleMenuService);
  private readonly locale = inject(LocaleService);
  private readonly bannerService = inject(AlertBannerService);
  private readonly theme = inject(ThemeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly texte = this.locale.messages;
  readonly username = this.auth.username;
  readonly loggedIn = this.auth.loggedIn;
  readonly themeMode = this.theme.mode;

  readonly menuEintraege = computed(() =>
    this.menu.visibleEntries(this.auth.userRoles())
  );

  readonly rollenChips = computed(() => {
    const labels = CVM_ROLE_LABELS as Readonly<Record<string, string>>;
    return this.auth
      .userRoles()
      .filter((r): r is CvmRole => r in labels)
      .map((r) => ({ key: r, label: labels[r] }));
  });

  readonly produkte: readonly { key: string; label: string }[] = [
    { key: 'PortalCore-Test', label: 'PortalCore-Test (1.14.2-test)' },
    { key: 'SmileKH-Test', label: 'SmileKH-Test' }
  ];

  selectedProduct = this.produkte[0]?.key ?? '';

  ngOnInit(): void {
    this.theme.init();
    this.auth.refreshFromKeycloak();
    // Banner nur pollen, wenn ein Login besteht. Sonst produzieren die
    // 401-Antworten Snackbar-Spam, ohne dass der Anwender etwas davon
    // hat - und (vor dem Interceptor-Fix) trieben sie die App in eine
    // Logout-Reload-Loop.
    if (this.auth.loggedIn()) {
      void this.bannerService.refresh();
    }
    interval(60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.auth.loggedIn()) {
          void this.bannerService.refresh();
        }
      });
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  async login(): Promise<void> {
    await this.auth.login();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
