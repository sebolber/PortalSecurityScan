# Iteration 20 – VEX-Export/Import + Waiver-Management

**Jira**: CVM-51
**Abhängigkeit**: 06
**Ziel**: Eure Relevanzaussagen maschinenlesbar an Dritte weitergeben und
zeitlich befristete Risiko-Akzeptanzen sauber verwalten.

---

## Kontext
Konzept v0.2 Abschnitt 10.3 (VEX-Export) und Abschnitt 11.5/11.6
(`validUntil`, Waiver-Management). VEX (Vulnerability Exploitability eXchange)
ist die maschinenlesbare Form dessen, was ihr heute manuell im Wiki
dokumentiert.

## Scope IN

### Teil A – VEX-Export/Import
1. **Export** in zwei Formaten:
   - CycloneDX-VEX 1.6 (bevorzugt, da eure SBOMs auch CycloneDX sind).
   - CSAF 2.0 (für Behörden-/Kundenanforderungen).
2. Mapping: jedes `APPROVED`-Assessment → ein VEX-Statement:
   - `not_affected` ← `NOT_APPLICABLE`
   - `affected` ← `CRITICAL|HIGH|MEDIUM|LOW` (mit Severity als Zusatz)
   - `fixed` ← `RESOLVED_BY_UPGRADE`
   - `under_investigation` ← `PROPOSED|NEEDS_REVIEW`
   - `justification`-Code gemäß VEX-Spezifikation abgeleitet aus
     `rationaleSourceFields` (z. B. `component_not_present`,
     `vulnerable_code_not_in_execute_path`).
3. **Import**: VEX-Dokumente Dritter konsumierbar, z. B. Upstream-VEX
   von OpenSSL oder Keycloak. Hinweise landen als **Vorschlag** in der
   Queue (`proposalSource=AI` mit Quelle `ADVISORY`), nicht als Bewertung.
4. REST:
   - `GET /api/v1/vex/{productVersionId}?format=cyclonedx|csaf`
   - `POST /api/v1/vex/import` (Multipart)

### Teil B – Waiver-Management
1. Waiver als eigene Entität `Waiver`:
   - Verknüpft mit `Assessment` mit Strategy `ACCEPT_RISK` oder
     `WAIT_UPSTREAM`.
   - Felder: `reason`, `grantedBy`, `validUntil`, `reviewInterval`
     (Default 90 Tage).
2. Flyway `V0013__waiver.sql`.
3. Waiver-Statusmaschine: `ACTIVE` → `EXPIRING_SOON` (30 Tage vor
   `validUntil`) → `EXPIRED`.
4. Scheduled-Job `WaiverLifecycleJob` (täglich):
   - Status-Übergänge.
   - Alert bei `EXPIRING_SOON` (Re-Use des Alert-Mechanismus aus
     Iteration 09).
   - Bei `EXPIRED`: Assessment wird auf `NEEDS_REVIEW` gesetzt,
     Re-Bewertung fällig.
5. Vier-Augen-Prinzip für Waiver-Anlage und -Verlängerung.
6. REST:
   - `POST /api/v1/waivers`
   - `POST /api/v1/waivers/{id}/extend`
   - `POST /api/v1/waivers/{id}/revoke`
   - `GET /api/v1/waivers?status=EXPIRING_SOON`
7. UI:
   - Neue Route `/waivers` mit Tabelle und Filter.
   - In Queue-Detail (Iteration 08): Hinweis bei aktivem Waiver.

## Scope NICHT IN
- Automatische Waiver-Verlängerung durch KI (bewusst nicht – Risiko-
  Akzeptanz ist immer menschliche Entscheidung).
- VEX-Roundtrip-Konformitätstests gegen offizielle Testsuites
  (Nice-to-have, später).

## Aufgaben
1. CycloneDX-VEX-Generator mit `cyclonedx-core-java` (unterstützt VEX).
2. CSAF-Generator (eigene JSON-Schema-Implementierung, Referenz:
   `csaf_security_advisory.json`).
3. VEX-Importer mit strenger Schema-Validierung; unbekannte Felder →
   Fehlermeldung, keine stille Akzeptanz.
4. Waiver-Anlage nur bei Bewertung mit passender Strategy, sonst
   Fehler `WaiverNotApplicableException`.

## Test-Schwerpunkte
- `VexExportTest`: alle Statuswerte korrekt gemappt, Signatur optional.
- `VexImportTest`: Happy-Path, Schema-Fehler, unbekannte Felder.
- `WaiverLifecycleJobTest` mit `Clock`-Abstraktion: ACTIVE → EXPIRING_SOON
  → EXPIRED; Alert-Trigger prüfbar.
- Vier-Augen-Test für Waiver-Anlage.
- Goldmaster-CycloneDX-VEX-Output für einen definierten Produktversionsstand.
- `@DisplayName`: `@DisplayName("VEX-Export: NOT_APPLICABLE wird zu not_affected mit passender Justification")`

## Definition of Done
- [ ] CycloneDX-VEX und CSAF exportierbar.
- [ ] VEX-Import funktioniert (als Vorschlag in Queue).
- [ ] Waiver-Lifecycle mit Alerts.
- [ ] Vier-Augen greift.
- [ ] Coverage `cvm-application/vex` und `.../waiver` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(vex): VEX-Export/Import und Waiver-Management ergaenzt\n\nCVM-51`

## TDD-Hinweis
Die Status-Übergänge des Waivers sind zeit-sensibel. Tests mit
`Clock.fixed(...)`-Injektion, **keine echten Zeitvergleiche**. Bei Rot
Produktionscode fixen, **nicht die Tests**.

## Abschlussbericht
Standard, plus Beispiel-VEX-Export als Anhang.
