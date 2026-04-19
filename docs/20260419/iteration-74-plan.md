# Iteration 74 - Plan: Persistente DRAFTs pro Umgebung

**Jira**: CVM-311

## Ziel

Iteration 64 hat Draft-Bearbeiten und Soft-Delete im Frontend
verdrahtet, setzt aber voraus, dass der DRAFT in derselben
Session angelegt wurde. Ueber Sessions hinweg kennt die UI
bestehende DRAFTs nicht. Ergebnis: der Admin sieht nach einem
Refresh nur noch das aktive Profil und kann seinen DRAFT nicht
weiter bearbeiten.

## Umfang

- **Backend**:
  - `ContextProfileService.latestDraftFor(UUID)` (read-only).
  - Neuer Endpunkt `GET /api/v1/environments/{id}/profile/draft`
    liefert den DRAFT mit der hoechsten Versionsnummer oder
    HTTP 404.
  - Zwei neue `ProfileControllerWebTest`-Cases.
- **Frontend**:
  - Neue Service-Methode `ProfilesService.aktuellerDraft(envId)`
    ueber `ApiClient.getOptional` (404 -> null).
  - In `ProfilesComponent.laden()` pro Row sowohl aktives Profil
    als auch aktuellen DRAFT nachladen; wenn ein DRAFT
    existiert, zusaetzlich den Diff gegen die aktive Version.
  - Zwei neue Service-Spec-Cases (200, 404).
  - Bestehende Component-Spec-Mocks um `aktuellerDraft`
    ergaenzen.

## Nicht-Umfang

- Kein Listing aller DRAFTs pro Umgebung. Ein Umgebung hat
  fachlich hoechstens einen offenen DRAFT; wenn das im Feld
  verletzt werden sollte, waere das ein Folge-Punkt.

## Abnahme

- `./mvnw -T 1C test` gruen.
- `npx ng lint`, `npx ng build`, Karma gruen.
