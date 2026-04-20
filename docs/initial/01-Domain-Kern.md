# Iteration 01 – Domain-Kern + Flyway

**Jira**: CVM-10
**Abhängigkeit**: 00
**Ziel**: Fachliche Kern-Entitäten anlegen, damit ab Iteration 02 Scans eingelesen werden können.

---

## Kontext
Lies `CLAUDE.md` und Konzept v0.2 Abschnitt 4 (Fachliche Kernkonzepte) und
Abschnitt 5 (Datenmodell) zuerst.

## Scope IN
Entities und Flyway-Migrationen für:
- `Product`, `ProductVersion`, `Environment`, `EnvironmentDeployment`
- `ContextProfile` (versioniert mit `validFrom`)
- `Scan`, `Component`, `ComponentOccurrence`
- `Cve` (globaler Stamm)
- `Finding` (Scan × Component-Occurrence × CVE)
- `Assessment` (immutable-versioniert, mit `proposalSource`-Enum; keine KI-Felder in dieser Iteration – aber Fremdschlüssel-Platzhalter `proposed_by_ai_suggestion_id` nullable anlegen, damit Iteration 11 nur noch die Ziel-Tabelle ergänzt)
- `MitigationPlan`
- `AuditTrail` ist bereits aus Iteration 00 vorhanden

JPA-Entities in `cvm-persistence`, Value Objects und Enums in `cvm-domain`.
Repositories als Spring-Data-Interfaces in `cvm-persistence`.

## Scope NICHT IN
- Rule-Engine-Tabelle (Iteration 05)
- KI-Tabellen (Iteration 11)
- REST-Controller (Iteration 02 und später)
- Keine Integrations-Clients

## Aufgaben
1. Flyway `V0001__produkt_und_version.sql`, `V0002__umgebung_und_profil.sql`,
   `V0003__scan_und_komponente.sql`, `V0004__cve_und_finding.sql`,
   `V0005__assessment_und_mitigation.sql`.
2. Enums in `cvm-domain/enums/`:
   `AhsSeverity`, `AssessmentStatus`, `ProposalSource`,
   `MitigationStrategy`, `MitigationStatus`, `EnvironmentStage`.
3. JPA-Entities mit `UUID id` (clientgeneriert via `@PrePersist`, nicht DB-seitig).
4. Alle Timestamps: `Instant` in DB (`timestamptz`), niemals `LocalDateTime`.
5. Repositories: `ProductRepository`, `ProductVersionRepository`,
   `EnvironmentRepository`, `ContextProfileRepository`, `ScanRepository`,
   `ComponentRepository`, `CveRepository`, `FindingRepository`,
   `AssessmentRepository`.
6. Domain-Services (Skelett): `ProductCatalogService`, `EnvironmentService`,
   `AssessmentLookupService`.
7. `@PrePersist` füllt `createdAt`, `@PreUpdate` füllt `updatedAt`. Für
   `Assessment`: keine Updates erlaubt (Versionierung), nur Insert neuer
   Zeilen. Constraint-Check per Entity-Listener.

## Test-Schwerpunkte
- `PersistenceIntegrationsTest` (Testcontainers): Speichern und Laden jeder
  Entity, Kardinalitäten prüfen.
- `AssessmentImmutableTest`: Versuch, ein bestehendes Assessment zu ändern,
  schlägt mit `ImmutabilityException` fehl.
- `FlywayMigrationReihenfolgeTest`: Alle Migrationen laufen durch, Schema
  vollständig.
- ArchUnit: `cvm-persistence` greift nicht nach `cvm-api`, `cvm-application`.
- `@DisplayName` auf Deutsch, Beispiel:
  `@DisplayName("ContextProfile: neue Version erhoeht versionsnummer und deaktiviert vorherige nicht automatisch")`

## Definition of Done
- [ ] Alle Migrationen laufen gegen Testcontainers-Postgres durch.
- [ ] Mindestens ein Repository-Test je Entity grün.
- [ ] `AssessmentImmutableTest` grün.
- [ ] ArchUnit grün.
- [ ] Coverage `cvm-persistence` ≥ 80 %.
- [ ] Fortschrittsbericht `docs/YYYYMMDD/iteration-01-fortschritt.md`.
- [ ] Commit: `feat(domain): Kern-Entities und Flyway-Baseline angelegt\n\nCVM-10`

## TDD-Hinweis
Beginne mit `PersistenceIntegrationsTest` (rot, kein Entity existiert),
implementiere dann Entity-für-Entity bis grün. **Ändere NICHT die Tests**,
wenn sie rot werden.

## Abschlussbericht
Wie in `CLAUDE.md` Abschnitt 8 beschrieben.
