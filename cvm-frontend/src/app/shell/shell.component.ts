import { Component, DestroyRef, OnInit, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth/auth.service';
import { MenuEntry, MenuSection, RoleMenuService } from '../core/auth/role-menu.service';
import { CVM_ROLE_LABELS, CvmRole } from '../core/auth/cvm-roles';
import { LocaleService } from '../core/i18n/locale.service';
import { AlertBannerService } from '../core/alerts/alert-banner.service';
import { ThemeService } from '../core/theme/theme.service';
import { BrandingHttpService } from '../core/theme/branding.service';
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
    MatExpansionModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatDividerModule,
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

  /**
   * Gruppiert die Menue-Eintraege in die drei Sektionen
   * (Workflow / Uebersicht / Einstellungen). Die Reihenfolge der
   * Eintraege innerhalb einer Sektion bleibt wie in
   * {@link RoleMenuService} definiert.
   */
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

  /**
   * UI-Fix HIGH-2: Tooltip-Zusammenfassung fuer die Rollen-Liste
   * im Header. Rollen werden im User-Menue voll angezeigt und hier
   * nur als "N Rollen"-Chip mit Hover-Tooltip, um den Header nicht
   * zu ueberladen.
   */
  readonly rollenTooltip = computed(() =>
    this.rollenChips()
      .map((r) => r.label)
      .join(', ')
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
      // Default-Branding bleibt aktiv. Kein Banner-Spam beim
      // anonymen Start.
    }
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
