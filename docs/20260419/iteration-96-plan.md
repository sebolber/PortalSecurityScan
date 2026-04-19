# Iteration 96 - Plan: Erstnutzer-Wizard (U-10)

**Jira**: CVM-336

## Ziel

Admin-User sollen bei der ersten Inbetriebnahme einen Wizard
durchlaufen, der sie Schritt fuer Schritt durch die vier
Grund-Artefakte fuehrt:

1. Produkt anlegen (`/admin/products`)
2. Umgebung anlegen (`/admin/environments`)
3. Kontext-Profil hinterlegen (`/profiles`)
4. Ersten Scan hochladen (`/scans/upload`)

Der Fortschritt wird in `localStorage` persistiert (Service
`OnboardingService`); bis der Wizard abgeschlossen ist, zeigt das
Dashboard eine prominente CTA.

## Umfang

### Neu
- `onboarding.service.ts` (+ Spec): State-Signal, `markDone`,
  `reset`, `completed`. LocalStorage-Key `cvm.onboarding.v1`,
  Versionierungs-Namespace fuer spaetere Migrationen.
- `onboarding.component.{ts,html,scss}` (+ Spec):
  Step-Kacheln mit Status-Icon, Fortschritts-Bar, CTA-Links zu den
  Ziel-Seiten, "Als erledigt markieren"-Buttons und "Zuruecksetzen".

### Wiring
- `/onboarding` Route (nur ADMIN, ueber `authGuard`).
- `RoleMenuService`: `ONBOARDING_ENTRY` wird als erstes Kind von
  "Einstellungen" eingehaengt.
- `DashboardComponent`: `zeigeOnboardingCta`-Computed, neue Card
  oberhalb der Handlungskarten mit "Weiter"-CTA.
- `role-menu.service.spec.ts`: expected child-IDs um `onboarding`
  ergaenzt.

## Nicht-Umfang

- Dynamische Vorpruefung, ob Produkte/Umgebungen wirklich
  existieren - aktuell manuelles "Als erledigt markieren".
- Keycloak-Flag "Onboarding abgeschlossen" o.a. - bleibt in
  localStorage pro Browser/Mandant.

## Abnahme

- `ng lint` / `ng build` / Karma gruen.
