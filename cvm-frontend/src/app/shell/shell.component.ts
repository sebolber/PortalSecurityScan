import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { MenuEntry, MenuSection, RoleMenuService } from '../core/auth/role-menu.service';
import { CVM_ROLE_LABELS, CvmRole } from '../core/auth/cvm-roles';
import { LocaleService } from '../core/i18n/locale.service';
import { AlertBannerService } from '../core/alerts/alert-banner.service';
import { ThemeService } from '../core/theme/theme.service';
import { BrandingHttpService } from '../core/theme/branding.service';
import { AlertBannerComponent } from './alert-banner.component';
import { CvmIconComponent } from '../shared/components/cvm-icon.component';

/**
 * Iteration 61B (CVM-62): Shell komplett auf Tailwind. Kein mat-toolbar,
 * kein mat-sidenav mehr. Sticky Topbar + feste linke Sidebar + freier
 * Content-Bereich mit voller Breite.
 */
@Component({
  selector: 'cvm-shell',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    AlertBannerComponent,
    CvmIconComponent
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
  private readonly branding = inject(BrandingHttpService);
  private readonly destroyRef = inject(DestroyRef);

  readonly texte = this.locale.messages;
  readonly username = this.auth.username;
  readonly loggedIn = this.auth.loggedIn;
  readonly themeMode = this.theme.mode;

  readonly brandingTitle = computed(
    () => this.theme.branding().appTitle ?? this.texte.app.title
  );
  readonly brandingLogoUrl = computed(() => this.theme.branding().logoUrl);
  readonly brandingLogoAlt = computed(
    () => this.theme.branding().logoAltText ?? 'CVM Dashboard'
  );

  readonly menuEintraege = computed(() =>
    this.menu.visibleEntries(this.auth.userRoles())
  );

  readonly menuGruppen = computed<
    readonly { section: MenuSection; label: string; entries: readonly MenuEntry[] }[]
  >(() => {
    const groups: Record<MenuSection, MenuEntry[]> = {
      workflow: [],
      uebersicht: [],
      einstellungen: []
    };
    for (const entry of this.menuEintraege()) {
      groups[entry.section].push(entry);
    }
    const ordered: {
      section: MenuSection;
      label: string;
      entries: MenuEntry[];
    }[] = [
      { section: 'workflow', label: 'Workflow', entries: groups.workflow },
      { section: 'uebersicht', label: 'Uebersicht', entries: groups.uebersicht },
      {
        section: 'einstellungen',
        label: 'Einstellungen',
        entries: groups.einstellungen
      }
    ];
    return ordered.filter((g) => g.entries.length > 0);
  });

  readonly rollenChips = computed(() => {
    const labels = CVM_ROLE_LABELS as Readonly<Record<string, string>>;
    return this.auth
      .userRoles()
      .filter((r): r is CvmRole => r in labels)
      .map((r) => ({ key: r, label: labels[r] }));
  });

  readonly rollenTooltip = computed(() =>
    this.rollenChips().map((r) => r.label).join(', ')
  );

  readonly produkte: readonly { key: string; label: string }[] = [
    { key: 'PortalCore-Test', label: 'PortalCore-Test (1.14.2-test)' },
    { key: 'SmileKH-Test', label: 'SmileKH-Test' }
  ];

  selectedProduct = this.produkte[0]?.key ?? '';

  readonly selectedProductLabel = computed(
    () =>
      this.produkte.find((p) => p.key === this.selectedProduct)?.label
        ?? this.texte.shell.productSelector
  );

  readonly productMenuOpen = signal(false);
  readonly userMenuOpen = signal(false);

  trackProduct(_: number, p: { key: string }): string {
    return p.key;
  }

  ngOnInit(): void {
    this.theme.init();
    this.auth.refreshFromKeycloak();
    if (this.auth.loggedIn()) {
      void this.bannerService.refresh();
      void this.ladeBranding();
    }
    interval(60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.auth.loggedIn()) {
          void this.bannerService.refresh();
        }
      });
  }

  private async ladeBranding(): Promise<void> {
    try {
      const config = await this.branding.load();
      this.theme.applyBranding(config);
    } catch {
      // Default-Branding bleibt aktiv.
    }
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  toggleProductMenu(): void {
    this.productMenuOpen.update((v) => !v);
    this.userMenuOpen.set(false);
  }

  toggleUserMenu(): void {
    this.userMenuOpen.update((v) => !v);
    this.productMenuOpen.set(false);
  }

  closeMenus(): void {
    this.productMenuOpen.set(false);
    this.userMenuOpen.set(false);
  }

  pickProduct(key: string): void {
    this.selectedProduct = key;
    this.productMenuOpen.set(false);
  }

  async login(): Promise<void> {
    await this.auth.login();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
