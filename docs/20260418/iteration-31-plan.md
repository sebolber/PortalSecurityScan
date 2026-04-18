# Iteration 31 – Plan

**Thema**: `branding_config_history` fuer One-Click-Rollback
**Jira**: CVM-72
**Datum**: 2026-04-18

## Hintergrund

Iteration 27 hat `branding_config` mit optimistic-Locking eingefuehrt,
aber kein Audit-/History-Modell. Ein Admin kann einen Branding-Fehler
(falsche Primaerfarbe, defekte Font-URL) nur manuell zurueckbauen -
der "One-Click-Rollback"-Wunsch aus den offenen Punkten der
Iteration 27 ist damit offen.

## Loesung

Jede erfolgreiche `BrandingService#updateForCurrentTenant` schreibt
zuerst den **alten** Stand als Zeile in `branding_config_history`
und saved danach den neuen Stand. Damit entsteht pro Mandant eine
historische Linie `(tenant_id, version, …)`.

Ein neuer `POST /api/v1/admin/theme/rollback/{version}` stellt eine
vergangene Version wieder her. Fachlich ist das wiederum ein normales
Update (mit WCAG-Pruefung und Historisierung), damit jeder Rollback
selbst auditierbar ist.

## Arbeitsschritte (TDD)

1. **Flyway V0027**: Tabelle `branding_config_history` mit allen
   Feldern aus `branding_config` plus `history_id`, `recorded_at`,
   `recorded_by`.
2. **Persistenz**: Entity `BrandingConfigHistory` + Repository
   (`findByTenantIdOrderByVersionDesc`,
   `findByTenantIdAndVersion`).
3. **Application**:
   - `BrandingService#updateForCurrentTenant` persistiert den
     **alten** Datensatz vor dem Speichern.
   - Neue Methode `rollbackForCurrentTenant(int targetVersion, String actor)`:
     liefert Zielversion aus History, baut `BrandingUpdateCommand`,
     ruft intern `updateForCurrentTenant`.
   - Neue Methode `history(int limit)` fuer die Admin-UI.
   - Tests: 2 neue (History beim Update, Rollback-Happy-Path) +
     Fehler (unbekannte Version).
4. **API**: `POST /api/v1/admin/theme/rollback/{version}` und
   `GET /api/v1/admin/theme/history` mit `@PreAuthorize('CVM_ADMIN')`.
   Web-Tests fuer Happy-Path + Fehler.
5. **Offene Punkte** aktualisieren.

## Stopp-Kriterien

- ArchUnit/Modulgrenzen (`api -> persistence`) duerfen nicht
  verletzt werden - History-Antwort wird ueber ein neues
  `BrandingHistoryEntry`-Record im Application-Modul gemappt.
- Flyway-V0027-Migration muss idempotent sein.
