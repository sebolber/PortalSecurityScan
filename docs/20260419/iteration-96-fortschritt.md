# Iteration 96 - Fortschritt

**Jira**: CVM-336 (U-10 Erstnutzer-Wizard)

## Umgesetzt

- `OnboardingService`: Signal-basiertes State-Management mit
  localStorage-Persistenz unter `cvm.onboarding.v1`. Vier
  Schritte (produkt, umgebung, profil, scan) in fester
  Reihenfolge; `markDone` befoerdert automatisch zum naechsten
  offenen Schritt. 6 Service-Specs.
- `OnboardingComponent`: neue Route `/onboarding`, vier Kacheln,
  Status-Icons, Fortschrittsbalken, Reset-Button. Jede Kachel
  verlinkt auf die echte Admin-Seite plus "Als erledigt
  markieren". 4 Component-Specs.
- `RoleMenuService`: `ONBOARDING_ENTRY` als erster Unterpunkt
  von "Einstellungen". Test erweitert um den neuen Child.
- `DashboardComponent`: neue CTA-Card `data-testid="dashboard-
  onboarding-cta"` erscheint fuer Admins, solange der Wizard
  nicht abgeschlossen ist. Zeigt `done.length / 4` und verlinkt
  auf `/onboarding`.

## Nicht umgesetzt

- Automatische Prefilling-Erkennung: der Wizard pruefte bewusst
  nicht, ob bereits Produkte/Umgebungen existieren - das koennte
  in einer Folge-Iteration aus dem Katalog ausgelesen werden,
  braucht aber zusaetzliche API-Calls im OnInit der
  OnboardingComponent.
- Persistenz in Keycloak/Backend: bleibt bewusst local per
  Browser, weil Mandantenwechsel haeufig an Browser gebunden
  sind.

## Technische Hinweise

- Der Service hat `typeof localStorage === 'undefined'`-Guard,
  damit SSR/SSG spaeter nicht bricht.
- Fortschritt wird als Prozent (0/25/50/75/100) berechnet und via
  `[style.width.%]` an die Bar gebunden.
