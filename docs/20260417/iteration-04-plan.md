# Iteration 04 – Arbeitsplan (Kontextprofil)

**Jira**: CVM-13
**Datum**: 2026-04-17
**Branch**: `claude/cve-relevance-manager-57JSm`

## Ziel
Umgebungen erhalten versionierte Kontextprofile (YAML, JSONB-persistiert),
Vier-Augen-Freigabe, Diff-getriebenes `NEEDS_REVIEW` an Assessments.

## Architektur-Entscheidungen

1. **Parsing**: `YAMLMapper` (jackson-dataformat-yaml, transitiv via
   json-schema-validator). SnakeYAML bleibt als weiterer Schutz
   (FAIL-safe gegen anchors/recursion). Ergebnis ist ein `JsonNode`.
2. **Schema-Validierung**: `com.networknt:json-schema-validator` (Draft 2020-12).
   Eingebettetes JSON-Schema als Classpath-Resource
   (`profile-schema-v1.json`). Fehlermeldungen werden auf Deutsch
   uebersetzt und feldgenau zurueckgegeben.
3. **Profil-Inhalt (Schema v1)**:
   ```yaml
   schemaVersion: 1
   umgebung:
     key: REF-TEST
     stage: REF
   architecture:
     windows_hosts: false
     linux_hosts: true
     kubernetes: true
   network:
     internet_exposure: false
     customer_access: true
   hardening:
     fips_mode: false
   compliance:
     frameworks: [ISO27001]
   ```
4. **State-Maschine**: neues Enum `ProfileState` in `cvm-domain`:
   `DRAFT`, `ACTIVE`, `SUPERSEDED`. Nur ein `ACTIVE` pro Umgebung
   (DB-seitiger Partial-Unique-Index).
5. **Diff-Builder**: tiefer, deterministischer Baum-Vergleich.
   Ausgabe: `List<ProfileFieldDiff(path, altWert, neuWert,
   changeType)>` mit `CREATED | REMOVED | CHANGED`. Pfade in
   Punkt-Notation (`architecture.windows_hosts`); Array-Diff via
   `[index]`-Suffix.
6. **Event-Listener**: `AssessmentReviewMarker` empfaengt
   `ContextProfileActivatedEvent`, holt alle Assessments mit
   `rationaleSourceFields` ∩ `changedPaths` ≠ ∅ und fuehrt eine
   batch-SQL-Aktualisierung (`@Modifying @Query "UPDATE assessment
   SET status='NEEDS_REVIEW' ..."`) durch. Das umgeht den
   `AssessmentImmutabilityListener` bewusst — hier handelt es sich
   nicht um eine fachliche Aenderung, sondern um eine System-
   Transition, die nachweislich auditierbar bleibt (neue Spalte
   `review_triggered_by_profile_version`).
7. **AssessmentStatus** erhaelt einen neuen Wert: `NEEDS_REVIEW`.
   Der DB-Check wird in V0008 entsprechend erweitert.
8. **Port-Trennung**: `cvm-api` kennt `cvm-application`, aber nicht
   `cvm-persistence` (ArchUnit-Regel). Controller spricht nur mit
   Services. Reponse-DTOs liegen in `cvm-api`.

## Flyway V0008
- `context_profile`: `state TEXT NOT NULL DEFAULT 'ACTIVE'`,
  `proposed_by TEXT`, `superseded_at TIMESTAMPTZ`,
  Check-Constraint `state IN ('DRAFT','ACTIVE','SUPERSEDED')`,
  Partial-Unique-Index `idx_profile_active_per_env` auf
  `(environment_id) WHERE state = 'ACTIVE'`.
- `assessment`: `rationale_source_fields JSONB`,
  `review_triggered_by_profile_version UUID`,
  Check `status IN ('PROPOSED','APPROVED','REJECTED','SUPERSEDED','NEEDS_REVIEW')`.

## Reihenfolge (TDD)

1. **Rot schreiben**:
   - `ProfileDiffBuilderTest` (6+ Faelle) – Unit, keine Spring-Dep.
   - `ContextProfileYamlParserTest` – happy + deutsche Fehlermeldung.
   - `ContextProfileServiceTest` – propose, approve, 4-Augen-Verstoss,
     diff-Event. Mockito-basiert.
   - `AssessmentReviewMarkerTest` – Event → batch-Update an Repo.
2. **Domain/Persistence**:
   - Enum `ProfileState`, `AssessmentStatus.NEEDS_REVIEW`.
   - `ContextProfile`-Entity erweitert, neuer Repository-Query
     `findActiveByEnvironmentId`.
   - `AssessmentRepository` erhaelt `markiereAlsReview(Set<UUID>,
     UUID profileVersionId)` als `@Modifying @Query`.
   - Flyway V0008.
3. **Application**:
   - `ProfileFieldDiff`, `ProfileDiffBuilder`,
     `ProfileSchemaValidator`, `ContextProfileYamlParser`,
     `ContextProfileService`, Events, Listener,
     `FourEyesViolationException`, `ProfileValidationException`.
4. **API**:
   - `ProfileController` (GET/PUT/POST-approve/GET-diff),
     DTOs, `ProfileExceptionHandler` (400/404/409).
5. **Abschluss**:
   - `./mvnw -T 1C test` laeuft gruen (Docker-ITs geskippt).
   - Reports schreiben, `offene-punkte.md` fortschreiben.

## Risiken / Annahmen
- `rationaleSourceFields` wird frueher eingefuehrt als urspruenglich
  gedacht, damit der Listener umsetzbar ist. Iteration 05/06 fuellt
  das Feld dann tatsaechlich.
- `@Modifying`-Update umgeht den Immutability-Listener. Das ist eine
  bewusste Systemtransition, die per Audit-Spalte nachverfolgbar
  bleibt. Iteration 06 kann ggf. einen `ReviewRequestedEvent`
  ergaenzen, sobald der Bewertungs-Workflow da ist.
- Kein UI in dieser Iteration (08 liefert die UI).
