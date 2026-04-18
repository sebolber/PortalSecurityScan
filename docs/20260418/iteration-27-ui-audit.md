# Iteration 27 - UI-Audit (Bestandsaufnahme)

**Stand**: 2026-04-18

Dieses Dokument listet den Ausgangszustand vor der
Theming-Ueberarbeitung. Es ist die Abnahmebasis fuer
Sebastian und die Grundlage der Migration.

## Methodik

Statische Code-Analyse ueber `cvm-frontend/src/app/**` sowie
`cvm-frontend/src/styles.scss`. Kein Screenshot-Audit moeglich
(Playwright/Browser in dieser Session nicht verfuegbar),
stattdessen Verweise auf Datei + Zeile.

## 1 Eigene Komponenten (selektiv, ohne `.spec.ts`)

| Name | Pfad | Zweck |
|---|---|---|
| `ShellComponent` | `shell/shell.component.ts` | App-Shell, Header, Sidebar |
| `AlertBannerComponent` | `shell/alert-banner.component.ts` | Eskalations-Banner (Iter 09) |
| `DashboardComponent` | `features/dashboard/dashboard.component.ts` | KPIs + Trend-Charts (Iter 07/21) |
| `QueueComponent` | `features/queue/queue.component.ts` | Bewertungs-Queue (Iter 08) |
| `QueueDetailComponent` | `features/queue/queue-detail.component.ts` | Finding-Detail + Copilot (Iter 08/14) |
| `QueueFilterSidebarComponent` | `features/queue/queue-filter-sidebar.component.ts` | Filter-Sidebar |
| `QueueTableComponent` | `features/queue/queue-table.component.ts` | Sortierbare Queue-Tabelle |
| `QueueHelpOverlayComponent` | `features/queue/queue-help-overlay.component.ts` | Shortcut-Overlay (`?`) |
| `CvesComponent` | `features/cves/cves.component.ts` | CVE-Browser (Iter 26) |
| `ComponentsComponent` | `features/components/components.component.ts` | Komponenten-Browser (Iter 26) |
| `ProfilesComponent` | `features/profiles/profiles.component.ts` | Aktives Profil je Umgebung |
| `RulesComponent` | `features/rules/rules.component.ts` | Regel-Liste/Editor (Iter 05) |
| `ReportsComponent` | `features/reports/reports.component.ts` | Report-Archiv (Iter 10/19) |
| `AiAuditComponent` | `features/ai-audit/ai-audit.component.ts` | KI-Call-Audit-Browser (Iter 11) |
| `SettingsComponent` | `features/settings/settings.component.ts` | Settings-Center (Iter 22) |
| `AhsButtonComponent` | `shared/components/ahs-button.component.ts` | Wrapper `mat-flat-button` |
| `AhsCardComponent` | `shared/components/ahs-card.component.ts` | Wrapper `mat-card` |
| `SeverityBadgeComponent` | `shared/components/severity-badge.component.ts` | Severity-Chip |
| `EmptyStateComponent` | `shared/components/empty-state.component.ts` | Leerzustand |
| `LoginCallbackComponent` | `login-callback/login-callback.component.ts` | OIDC-Redirect |

## 2 Angular-Material-Direktnutzung (Top-Verbraucher)

Direkt in Feature-Komponenten importiert, ohne Durchlauf
durch `shared/ui/`:

- `MatToolbarModule`, `MatSidenavModule`, `MatListModule`
  (`shell/shell.component.ts`)
- `MatTableModule`, `MatPaginatorModule`, `MatChipsModule`
  (`features/cves/`, `features/queue/queue-table.component.ts`,
  `features/ai-audit/`, `features/reports/`)
- `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`
  (in fast jeder Feature-Component)
- `MatCardModule`, `MatButtonModule`, `MatIconModule`
  (allgegenwaertig)
- `MatExpansionModule` (`profiles/`, `settings/`, `rules/`)
- `MatButtonToggleModule`, `MatSlideToggleModule`,
  `MatTooltipModule` (`cves/`, `queue/`)
- `MatProgressSpinnerModule` (Loading-States in fast
  allen Feature-Components)

Jede dieser Direktnutzungen muss in Iteration 27b durch eine
`Ahs*`-Komponente ersetzt oder zumindest mit Token-Bindings
vereinheitlicht werden.

## 3 Inline-Styles und hardkodierte Werte (Auszug)

| Datei | Zeile | Befund |
|---|---|---|
| `styles.scss` | 44-66 | `:root`-Variablen mit Hex-Rohwerten (`#e2001a`, `#1c1c1c`), kein Token-Layer |
| `styles.scss` | 71 | `font-family: Roboto, 'Helvetica Neue', sans-serif` hartkodiert |
| `shell/shell.component.scss` | vorhandene `px`- und Farbwerte (Logo-Kreis, Sidebar) |
| `shared/components/empty-state.component.ts` | 9-10 | Tailwind-Utility `text-zinc-500`, `text-4xl` - kein Severity-Token |
| `shared/components/ahs-card.component.ts` | 29-30 | Inline `box-shadow: 0 4px 16px rgba(0,0,0,0.08)` |
| `shared/components/ahs-button.component.ts` | 28-36 | Mapping auf `'primary' \| 'accent' \| 'warn'` - keine Danger/Ghost-Variante |
| Feature-SCSSs | diverse | `padding: 16px`, `gap: 12px`, Hex-Werte und Tailwind-Arbitrary-Values. Siehe `git grep -nE '#[0-9a-fA-F]{3,6}' cvm-frontend/src/app` |

## 4 Ueberlappungs-/Proportions-Problemstellen (Heuristik)

Aus Codeinspektion abgeleitete Verdachtsstellen, die der
Besteller im Browser gesehen hat. Kein Screenshot moeglich,
Fix erfolgt im Rahmen 27b.

| Stelle | Datei | Verdacht |
|---|---|---|
| Shell-Produktwahl | `shell/shell.component.html:9` | `MatFormField` im Toolbar - Label floatet, kann im kompakten Zustand ueberlappen |
| Queue-Filter-Sidebar | `queue-filter-sidebar.component.ts` | Horizontales Chip-Stapeln ohne Wrap-Container |
| CVE-Tabelle | `features/cves/cves.component.html` | Spalten ohne Mindestbreite - Summary-Zelle verdraengt CVSS/Severity |
| Settings-Formulare | `features/settings/settings.component.html` | Einzelne Rubriken nutzen `MatFormField` ohne `pageForm`-Wrapper, dadurch Label-Input-Ueberlappung bei engen Viewports |
| Logo | `shell/shell.component.html:3-6` | Logo links statt rechts (laut Besteller-Wunsch rechts), `mat-icon` skaliert nicht mit Header-Hoehe |
| Empty-States | `shared/components/empty-state.component.ts` | Icon-Groesse Inline-Style `[style.fontSize.px]="40"` - bricht Token-Regel |
| Severity-Farben | Charts/Tabellen | Keine zentrale Severity-Palette, jede Komponente zieht eigene Farben |

## 5 Coverage-Beobachtungen (Hinweis auf 2.0)

- **CVEs** (`/cves`), **Komponenten** (`/components`) sind
  mit Iteration 26 angebunden - kein Leerzustand mehr.
- **Profile** (`/profiles`) zeigt Liste der aktiven Profile
  pro Umgebung - angebunden.
- **Regeln** (`/rules`), **Reports** (`/reports`),
  **KI-Audit** (`/ai-audit`), **Settings** (`/settings`)
  haben Komponenten mit Datenbezug.
- Neu als Platzhalter in dieser Iteration: Theme-Admin-UI
  `/admin/theme` (wird in 27 geliefert).

Vollstaendige Liste: siehe `frontend-backend-coverage.md`.

## 6 Empfehlung fuer die Migration

1. Token-Layer zuerst anlegen und in `styles.scss`
   importieren - das liefert sofort CSS-Variablen, die in
   bestehenden Komponenten ohne Refactor genutzt werden
   koennen.
2. `shared/ui/` um `AhsPagePlaceholder`, `AhsBanner`
   erweitern, damit `FullNavigationWalkThroughTest` in
   Folge-Iteration 27b schnell gruen wird.
3. Feature-Bereiche in der von Iteration 27 Abschnitt 2.8
   vorgegebenen Reihenfolge migrieren. Pro Bereich
   eigener Commit, pro Bereich Vorher-/Nachher-Screenshot.
