# Iteration 61 - Migrations-Leitfaden (Material -> Tailwind)

Kurzreferenz fuer alle Feature-Migrationen. Jede Feature-Komponente folgt
dieser Ersetzungs-Tabelle 1:1.

## Verfuegbare Primitive

- `CvmIconComponent` (`../../shared/components/cvm-icon.component`) - Selector `cvm-icon`, Input `name`, `size` (Default 20).
- `CvmDialogComponent` (`../../shared/components/cvm-dialog.component`) - Selector `cvm-dialog`, Input `open`, `title`, `size` (`sm|md|lg|xl`), Output `close`. Content per Slot, Footer per `[footer]`-Attribut.
- `CvmDrawerComponent` (`../../shared/components/cvm-drawer.component`) - Selector `cvm-drawer`, analog.
- `CvmToastService` (`../../shared/components/cvm-toast.service`) - Methoden `info/success/warning/error(text)`. Ersetzt `MatSnackBar`.
- `AhsButtonComponent` (`../../shared/components/ahs-button.component`) - bleibt, ist jetzt pure Tailwind.
- `AhsCardComponent`, `AhsBannerComponent`, `SeverityBadgeComponent`, `EmptyStateComponent`, `PagePlaceholderComponent`, `UuidChipComponent` - alle pure Tailwind.

## CSS-Klassen aus `styles.scss`

Buttons: `.btn .btn-primary|.btn-secondary|.btn-ghost|.btn-danger`, `.btn-icon`, `.btn-sm`, `.btn-lg`
Inputs: `.input-field`, `.textarea-field`, `.select-field`, `.form-label`, `.form-help`, `.form-error`, `.form-group`, `.filter-bar`
Cards: `.card`, `.card-padded`, `.card-header`, `.card-title`, `.card-body`, `.card-footer`
Badges: `.badge`, `.badge-neutral|.badge-info|.badge-success|.badge-warning|.badge-danger`
Severity: `.severity-chip[data-sev="CRITICAL|HIGH|MEDIUM|LOW|INFORMATIONAL|NOT_APPLICABLE"]`
Table: `.table-card`, `.data-table`, `.data-table--compact`
Dialog: `.dialog-overlay`, `.dialog-panel`, `.dialog-header`, `.dialog-title`, `.dialog-body`, `.dialog-footer`
Drawer: `.drawer-panel`
Banner: `.banner .banner-info|.banner-success|.banner-warning|.banner-critical`
Tabs: `.tabs`, `.tab`, `.tab-active`
Page: `.page`, `.page-header`, `.page-title`, `.page-subtitle`, `.page-section`

## TS-Import-Ersetzungen

**ENTFERNEN** aus jeder Feature-Komponente:
- `MatButtonModule`, `MatIconModule`, `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`, `MatCardModule`, `MatTableModule`, `MatPaginatorModule`, `MatSlideToggleModule`, `MatCheckboxModule`, `MatProgressSpinnerModule`, `MatChipsModule`, `MatTooltipModule`, `MatButtonToggleModule`, `MatExpansionModule`, `MatDividerModule`, `MatMenuModule`, `MatSnackBar`, `MatDialog`, `MatDialogRef`, `MAT_DIALOG_DATA`, `MatDialogModule`, `MatProgressBarModule`, `MatSidenavModule`, `MatListModule`, `MatToolbarModule`, `MatRadioModule`, `MatDatepickerModule`, `MatNativeDateModule`

**ERGAENZEN** (je nach Bedarf):
- `import { CvmIconComponent } from '../../shared/components/cvm-icon.component';`
- `import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';`
- `import { CvmToastService } from '../../shared/components/cvm-toast.service';`
- `import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';`
- `import { EmptyStateComponent } from '../../shared/components/empty-state.component';`

## Template-Ersetzungen

| Material | Tailwind-Equivalent |
|---|---|
| `<mat-toolbar>` | `<header class="cvm-topbar">` (Shell) |
| `<mat-sidenav>` | `<aside class="cvm-sidebar">` (Shell) |
| `<mat-icon>X</mat-icon>` | `<cvm-icon name="X" [size]="18"></cvm-icon>` |
| `<mat-icon matSuffix>X</mat-icon>` | absolut-positionierter `cvm-icon` im Input-Wrapper |
| `<button mat-button>` | `<button class="btn btn-ghost">` |
| `<button mat-stroked-button>` | `<button class="btn btn-secondary">` |
| `<button mat-flat-button color="primary">` | `<button class="btn btn-primary">` |
| `<button mat-raised-button color="primary">` | `<button class="btn btn-primary">` |
| `<button mat-raised-button color="warn">` | `<button class="btn btn-danger">` |
| `<button mat-icon-button>` | `<button class="btn-icon">` |
| `<mat-card>` | `<section class="card">` |
| `<mat-card-content>` | `<div class="card-body">` |
| `<mat-card-header>`/`<mat-card-title>` | `<header class="card-header"><h2 class="card-title">` |
| `<mat-form-field appearance="outline"><mat-label>X</mat-label><input matInput [(ngModel)]="v">` | `<label class="form-group"><span class="form-label">X</span><input class="input-field" [(ngModel)]="v"/></label>` |
| `<mat-form-field><mat-select>` | `<label class="form-group"><span class="form-label">X</span><select class="select-field">` |
| `<mat-slide-toggle [(ngModel)]="v">Text</mat-slide-toggle>` | `<label class="inline-flex items-center gap-2"><input type="checkbox" class="form-checkbox" [(ngModel)]="v"/><span class="text-sm">Text</span></label>` |
| `<mat-checkbox [(ngModel)]="v">Text</mat-checkbox>` | analog (Checkbox) |
| `<mat-radio-group>` / `<mat-radio-button>` | `<label class="inline-flex items-center gap-2"><input type="radio" name="..." value="..." [(ngModel)]="v"/>Text</label>` |
| `<mat-button-toggle-group>` / `<mat-button-toggle>` | `<div class="inline-flex rounded-lg border border-border bg-surface overflow-hidden h-10"><button class="px-3 text-sm border-r border-border last:border-r-0" [class.bg-primary-muted]="...">X</button></div>` |
| `<mat-progress-spinner>` / `<mat-spinner>` | `<cvm-icon name="loader" [size]="20" class="animate-spin"></cvm-icon>` |
| `<mat-progress-bar>` | `<div class="h-2 rounded-full bg-surface-muted overflow-hidden"><div class="h-full bg-primary" [style.width.%]="value"></div></div>` |
| `<mat-chip>` | `<span class="badge badge-neutral">` |
| `<mat-chip color="primary">` | `<span class="badge badge-info">` |
| `<mat-tab-group>` / `<mat-tab>` | `<div class="tabs"><button class="tab" [class.tab-active]="i===active" (click)="active=i">` |
| `<mat-table [dataSource]>` + `<ng-container matColumnDef="X">` | `<table class="data-table"><thead>...</thead><tbody>...</tbody></table>` |
| `<mat-paginator>` | Eigene `prev/next + Anzahl`-Leiste (siehe CVEs-Template). |
| `<mat-expansion-panel>` | `<details>`, `<summary>` |
| `<mat-divider>` | `<hr class="border-border">` |
| `<mat-menu>` + `[matMenuTriggerFor]` | absolut-positioniertes `<div class="cvm-menu">` mit Signal `open` |
| `[matTooltip]` | `[attr.title]` |
| `<mat-dialog>` | `<cvm-dialog [open]="...">` |

## Dialog-Ersatz (MatDialog -> CvmDialog)

Wenn `MatDialog.open(...)` verwendet wird:
- Statt Dialog-Komponente per `MatDialog.open` oeffnen: den Dialog inline im Host-Template einbinden mit `<cvm-dialog [open]="zeigeDialog()" (close)="schliessen()">` und einem Signal im Host-Component.
- `MatDialogRef.close(result)` -> Host-Component bekommt das Ergebnis als Event oder Methode `onConfirm()` direkt.
- Der Dialog-Inhalt wird zu einer Standalone-Component, die `@Input`/`@Output` statt `MAT_DIALOG_DATA`/`MatDialogRef` verwendet.

## SnackBar-Ersatz (MatSnackBar -> CvmToastService)

```ts
// alt:
this.snack.open('X', 'OK', { duration: 2000 });
// neu:
this.toast.success('X', 2000);
```

## Paginierungs-Pattern

Statt `<mat-paginator>`:
- Signal `pageIndex`, Konstante `pageSize`, Methoden `prevPage()`/`nextPage()`.
- Template: `x-y von z` + Pfeil-Buttons.

## ECharts / Monaco / Keycloak

- `ngx-echarts` bleibt unveraendert, Charts laufen.
- `ngx-monaco-editor-v2` bleibt unveraendert.
- `keycloak-angular` bleibt.

## Fluchten / Layout-Regeln

- Filter-Zeilen: IMMER `<div class="filter-bar">` (flex, items-end, gap-3).
- Alle Controls (Input, Select, Button) in der Zeile haben `h-10`. Der `form-group`-Wrapper sorgt dafuer, dass Labels fluchten.
- Seitenrahmen: `<div class="page">` (6/8 padding, Spalten-Flow).
- Detail-Seiten: `w-full`, kein `max-w-*`. Bei Bedarf `grid grid-cols-12 gap-6` fuer Sub-Bereiche.

## Dialog-Beispiel

```html
<cvm-dialog [open]="showApprove()" title="Assessment freigeben" size="md" (close)="cancel()">
  <div class="flex flex-col gap-4">
    <div class="form-group">
      <label class="form-label form-label--required">Begruendung</label>
      <textarea class="textarea-field" [(ngModel)]="reason"></textarea>
      <p class="form-help">Wird im Audit-Trail gespeichert.</p>
    </div>
  </div>
  <div footer>
    <button class="btn btn-secondary" (click)="cancel()">Abbrechen</button>
    <button class="btn btn-primary" (click)="approve()" [disabled]="!reason">Freigeben</button>
  </div>
</cvm-dialog>
```
