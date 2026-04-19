# Iteration 50 - Fortschritt

**Thema**: Regeln Soft-Delete mit Abgrenzung zu RETIRED (CVM-100).

## Was gebaut wurde

- Flyway V0033 `V0033__rule_soft_delete.sql` mit ausfuehrlichem
  SQL-Kommentar zur Abgrenzung `deleted_at` vs. `retired_at`.
- `Rule.deletedAt` + Javadoc-Abgrenzung.
- Repository-Methoden
  `findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc` und
  `findByDeletedAtIsNullOrderByCreatedAtDesc`.
- `RuleEngine` nutzt die neue Status+DeletedAt-Query (soft-geloeschte
  Regeln werden nicht mehr ausgewertet).
- `RuleService.listAll()` filtert deletedAt, neuer `loesche(UUID)`.
- `RulesController.DELETE /api/v1/rules/{ruleId}` (`CVM_ADMIN`).
- Frontend:
  - `rulesService.delete(id)`.
  - `rules.component` zeigt den Loesch-Button nur fuer Admins, mit
    erklaerendem `window.confirm` (Soft-Delete vs. RETIRED).
- `RuleEngineTest` angepasst: nutzt die neue Repository-Methode.

## Neue Tests

- `RuleServiceTest` +4 Faelle (Happy-Path, Idempotenz, unbekannt,
  null).

## Build

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; 339 Tests, BUILD
  SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; 147 Tests, BUILD
  SUCCESS.
- `npx ng build` &rarr; ok.
- `npx ng lint` &rarr; All files pass linting.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0033**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` laufen.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Ja; der Button "Soft-Delete"
   steht neben "Aktivieren", Tooltip erklaert die Abgrenzung zu
   RETIRED.
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Ja; SnackBar
   meldet die Loeschung und die Regel-Liste wird neu geladen
   (Regel verschwindet).
3. *Sind Daten sichtbar, die im Backend existieren?* Ja;
   geloeschte Regeln sind aus der Liste entfernt, aber historische
   Assessments behalten ueber `findById` weiter ihre Rationale.
4. *Gibt es einen Weg zurueck/weiter?* Soft-Delete per Bestaetigungs-
   Dialog; bei versehentlicher Loeschung bleibt die Regel
   datenbankseitig erhalten und kann per Admin-SQL
   (`UPDATE rule SET deleted_at = NULL WHERE id = ?`)
   reaktiviert werden.
