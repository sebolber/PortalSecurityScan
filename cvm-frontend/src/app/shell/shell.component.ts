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
import { TenantsService, TenantView } from '../core/tenants/tenants.service';
import { AlertBannerComponent } from './alert-banner.component';
import { CvmBreadcrumbsComponent } from '../shared/components/cvm-breadcrumbs.component';
import { CvmIconComponent } from '../shared/components/cvm-icon.component';
import { GlobalShortcutsDirective } from '../shared/components/global-shortcuts.directive';
import { GlobalShortcutsOverlayComponent } from '../shared/components/global-shortcuts-overlay.component';

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
    CvmBreadcrumbsComponent,
    CvmIconComponent,
    GlobalShortcutsDirective,
    GlobalShortcutsOverlayComponent
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
  private readonly tenants = inject(TenantsService);
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

  /**
   * Iteration 62 (CVM-62): Aktueller Mandant aus dem JWT. Wird aus
   * `/api/v1/tenant/current` geladen; ein Switch ist hier bewusst
   * nicht moeglich, weil der Mandant ueber das Keycloak-Token und
   * den {@code TenantContextFilter} gesetzt wird. Wechsel = erneuter
   * Login mit anderem Mandanten-Kontext.
   */
  readonly tenantSig = signal<TenantView | null>(null);
  readonly tenantLabel = computed<string>(() => {
    const t = this.tenantSig();
    return t ? t.name : this.texte.app.notLoggedIn;
  });

  readonly userMenuOpen = signal(false);

  // Iteration 91 (CVM-331): Globale Shortcut-Sheet.
  readonly shortcutsOpen = signal(false);

  // Iteration 84 (CVM-324): Tenant-Popover mit Liste und
  // Set-Default-Aktion fuer Admins.
  readonly tenantMenuOpen = signal(false);
  readonly alleTenants = signal<readonly TenantView[]>([]);
  readonly tenantMenuLaedt = signal(false);
  readonly istAdmin = computed(() =>
    this.auth.userRoles().includes('CVM_ADMIN')
  );

  ngOnInit(): void {
    this.theme.init();
    this.auth.refreshFromKeycloak();
    if (this.auth.loggedIn()) {
      void this.bannerService.refresh();
      void this.ladeBranding();
      void this.ladeTenant();
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

  private async ladeTenant(): Promise<void> {
    try {
      this.tenantSig.set(await this.tenants.current());
    } catch {
      this.tenantSig.set(null);
    }
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  oeffneShortcutSheet(): void {
    this.shortcutsOpen.set(true);
  }

  schliesseShortcutSheet(): void {
    this.shortcutsOpen.set(false);
  }

  toggleUserMenu(): void {
    this.userMenuOpen.update((v) => !v);
    if (this.userMenuOpen()) {
      this.tenantMenuOpen.set(false);
    }
  }

  toggleTenantMenu(): void {
    const naechster = !this.tenantMenuOpen();
    this.tenantMenuOpen.set(naechster);
    if (naechster) {
      this.userMenuOpen.set(false);
      if (this.istAdmin()) {
        void this.ladeAlleTenants();
      }
    }
  }

  closeMenus(): void {
    this.userMenuOpen.set(false);
    this.tenantMenuOpen.set(false);
  }

  private async ladeAlleTenants(): Promise<void> {
    this.tenantMenuLaedt.set(true);
    try {
      this.alleTenants.set(await this.tenants.list());
    } catch {
      this.alleTenants.set([]);
    } finally {
      this.tenantMenuLaedt.set(false);
    }
  }

  async setzeAlsDefault(tenantId: string): Promise<void> {
    try {
      await this.tenants.setDefault(tenantId);
      await this.ladeAlleTenants();
      await this.ladeTenant();
    } catch {
      // Fehler-Toast waere schoen - bleibt Folge-Iteration. Noop.
    }
  }

  async login(): Promise<void> {
    await this.auth.login();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
