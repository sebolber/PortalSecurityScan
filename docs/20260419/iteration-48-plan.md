# Iteration 48 - Umgebungen Soft-Delete

## Ziel

Umgebungen koennen analog zum Produkt-Soft-Delete (Iteration 38)
logisch entfernt werden: bestehende Scans, Findings und Assessments
referenzieren die Umgebung weiter, die Eintraege verschwinden nur
aus den Admin-/Auswahl-Listen.

## Vorgehen

1. **Flyway V0031** (`environment_soft_delete.sql`): nullable-Spalte
   `deleted_at TIMESTAMPTZ` + Partial-Index auf
   `deleted_at IS NOT NULL`.
2. **Entity**: `Environment.deletedAt`.
3. **Repository**: neue Methode
   `findByDeletedAtIsNullOrderByKeyAsc()`.
4. **Service** `EnvironmentQueryService`:
   - `listAll()` liest nur aktive Umgebungen.
   - neue Methode `loesche(UUID)` setzt `deletedAt` (idempotent).
5. **Controller**: `DELETE /api/v1/environments/{id}`
   (`CVM_ADMIN`), 204 No Content.
6. **ExceptionHandler**: `EntityNotFoundException` &rarr; 404.
7. **Frontend**: Icon-Button in der Umgebungs-Tabelle, neuer
   `service.delete(id)`, `window.confirm` analog
   `admin-products`.
8. **Tests**: Service-Tests fuer `loesche` (happy/idempotent/not-
   found/null), Service-Test fuer das Filtern beim Listen.

## Nicht-Ziele

- Resurrection / Undelete-UI. Bleibt Admin-SQL, analog Produkt.

## Testerwartung

- `./mvnw -T 1C -pl cvm-application,cvm-api,cvm-app -am test` &rarr;
  BUILD SUCCESS.
- `npx ng build` und `npx ng lint` gruen.
- **Vor dem naechsten `scripts/start.sh`** muss
  `./mvnw -T 1C clean install -DskipTests` laufen (neue Flyway-
  Migration V0031).

## Jira

`CVM-98` - Umgebungen Soft-Delete.
