# Iteration 24 - UI-Ueberarbeitung + Theming - Plan

**Jira**: CVM-55
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Auftrag

Nach Iteration 23 (Keycloak-Rollen scharf) werden die Frontend-
Rollen-Konstanten an den neuen Realm angepasst und das Theming
ueberarbeitet. Ziel: Adesso-CI-konforme Shell mit Light-/Dark-
Mode-Umschalter, Rollen-Chips im Userpanel und rollenbasierter
Navigation.

## Scope

1. **Rollen-Konstanten (`cvm-roles.ts`)** synchron mit Realm
   - Ergaenze: `CVM_REVIEWER`, `CVM_PROFILE_AUTHOR`,
     `CVM_PROFILE_APPROVER`, `CVM_RULE_AUTHOR`,
     `CVM_RULE_APPROVER`, `CVM_REPORTER`.
   - Entferne den nicht im Realm existierenden
     `PRODUCT_OWNER`.
   - `CvmRole`-Typ bleibt ein String-Union.

2. **RoleMenuService** neu ausrichten
   - Profile-Eintrag: `PROFILE_AUTHOR|PROFILE_APPROVER|ADMIN`.
   - Rules-Eintrag: `RULE_AUTHOR|RULE_APPROVER|ADMIN`.
   - Reports-Eintrag: `VIEWER|REPORTER|ADMIN`.
   - Queue-Eintrag: `ASSESSOR|REVIEWER|APPROVER|ADMIN`.
   - AI-Audit-Eintrag: unveraendert
     `AI_AUDITOR|ADMIN`.
   - Spec-Datei mit den neuen Erwartungen anpassen.

3. **Routen-Guards** (`app.routes.ts`) entsprechend.

4. **ThemeService** (`core/theme`)
   - Signal `mode: 'light' | 'dark'`.
   - Persistenz via `localStorage` (`cvm.theme`).
   - Setzt `data-theme`-Attribut am `<html>`-Element.
   - Toggle-Methode `toggle()`.
   - Unit-Test (setzt Attribut + Persist).

5. **Theme-CSS** (`styles.scss`)
   - CSS-Variablen fuer Hintergrund, Surface, Textfarbe.
   - Light: adesso-light, Dark: dunkel-neutrales Grau mit
     Rot-Akzent.
   - `html[data-theme="dark"]` setzt die Dark-Variablen.
   - Tailwind `dark:`-Variante via `darkMode: ['class',
     '[data-theme="dark"]']`.

6. **Shell polieren**
   - Linker Logo-Block bekommt den Namen "CVM" neben dem
     Shield-Icon, klar lesbar.
   - Userpanel zeigt `username` PLUS aktive Rollen als
     kleine Chips (`mat-chip-option-like`, aber leichtgewichtig
     per `<span>`-Badges, kein zusaetzliches Material-Modul).
   - Theme-Toggle als `mat-icon-button` mit `light_mode`/
     `dark_mode`-Icon neben dem Userpanel.
   - Active-Link-Farbe aus CSS-Variablen.

7. **Tests**
   - `ThemeService` Unit-Test.
   - `role-menu.service.spec` aktualisieren (neue
     Erwartungen).
   - `ng build` + `ng lint` bleiben gruen.

## Nicht in dieser Iteration

- Ueberarbeitung der Einzelseiten (Queue-Table-Redesign,
  Dashboard-Widgets, etc.) - das ist pro Feature separat.
- i18n-EN-Texte (weiterhin offener Punkt).
- Keycloak-UI-Theme.
- PWA / Offline-Modus.
