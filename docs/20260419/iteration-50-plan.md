# Iteration 50 - Regeln Soft-Delete

## Ziel

Regeln koennen analog zu Produkten, Produkt-Versionen und Umgebungen
logisch entfernt werden. Abgrenzung zum Status `RETIRED`:

- `RETIRED` = fachlich abgeloest (eine neue Regel hat sie ersetzt;
  bleibt im Audit-Trail sichtbar).
- `deleted_at != NULL` = technisch entfernt (Admin-Cleanup);
  Regel-Engine beruecksichtigt sie nicht mehr; Regel-Liste blendet sie
  aus.

## Vorgehen

1. **Flyway V0033** (`rule_soft_delete.sql`).
2. **Rule** bekommt `deletedAt`.
3. **Repository** erhaelt
   - `findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status)`
     (Eingabe der Regel-Engine).
   - `findByDeletedAtIsNullOrderByCreatedAtDesc()` fuer die Admin-
     Liste.
4. **RuleEngine** nutzt die gefilterte Query.
5. **RuleService**:
   - `listAll()` filtert deletedAt.
   - neue `loesche(UUID)` - idempotent, `RuleNotFoundException` bei
     unbekannter ID, `IllegalArgumentException` bei null.
6. **Controller** `DELETE /api/v1/rules/{ruleId}` (`CVM_ADMIN`).
7. **Frontend**: `rules.service.delete`, Loesch-Button pro Regel
   (nur fuer Admin) mit erklaerendem `window.confirm`.
8. Tests.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application -am test` &rarr; BUILD SUCCESS.
- `./mvnw -T 1C -pl cvm-app,cvm-api -am test` &rarr; BUILD SUCCESS.
- `npx ng build` + `npx ng lint` gruen.

## Wichtig fuer den naechsten Start

- **Neue Flyway-Migration V0033**: vor dem naechsten
  `scripts/start.sh` muss
  `./mvnw -T 1C clean install -DskipTests` laufen.

## Jira

`CVM-100` - Regeln Soft-Delete (Abgrenzung zu RETIRED).
