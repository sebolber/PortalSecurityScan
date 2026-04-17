# Prompt fuer die naechste Claude-Code-Session (Iteration 06+)

Kopiere den Block zwischen den `---` in ein neues Kontextfenster, sobald
Iteration 06 (Bewertungs-Workflow, CVM-15) starten soll.

---

Du bist Claude Code und arbeitest am CVE-Relevance-Manager (CVM) der adesso
health solutions GmbH. Repo: sebolber/PortalSecurityScan.

## Stand
Iterationen 00-05 sind abgeschlossen und auf `main` (letzter Commit
`51ad661`).

Erledigt:
- 00 Projekt-Bootstrap (Maven-Multi-Modul, Spring Boot 3.3.5, Java 21,
  docker-compose, ArchUnit, Angular-18-Shell, GitLab-CI).
- 01 Domain-Kern (Entities fuer Product/Version/Environment/
  ContextProfile/Scan/Component/ComponentOccurrence/Cve/Finding/
  Assessment/MitigationPlan, Flyway V0001-V0005, Immutability-Listener).
- 02 SBOM-Ingestion (CycloneDX-Parser, AES-GCM-SbomEncryption,
  ScanIngestService, ScanIngestedEvent, REST /api/v1/scans,
  ExceptionHandler 400/404/409/503, Flyway V0006).
- 03 CVE-Anreicherung (NVD/GHSA/KEV/EPSS-Adapter, Bucket4j-Rate-Limiter,
  CveEnrichmentPort, CveEnrichmentService idempotent mit 24h-Cache,
  Listener auf ScanIngestedEvent, Scheduled-Jobs, Admin-Endpoint,
  Flyway V0007).
- 04 Kontextprofil (YAML + JSON-Schema-Validierung via
  networknt, ProfileState DRAFT/ACTIVE/SUPERSEDED, ProfileDiffBuilder,
  ContextProfileActivatedEvent, AssessmentReviewMarker setzt betroffene
  Assessments per @Modifying-Query auf NEEDS_REVIEW, neue Enum-Stufe
  AssessmentStatus.NEEDS_REVIEW, REST unter
  /api/v1/environments/{id}/profile und /api/v1/profiles/{id}/approve
  + /diff, Flyway V0008).
- 05 Regel-Engine (JSON-DSL mit 8 Skalar-Ops + all/any/not, sealed
  ConditionNode-AST, PathResolver fuer cve./profile./component./
  finding., RuleEvaluator, RationaleTemplateInterpolator, RuleEngine,
  RuleService mit Vier-Augen-Aktivierung, DryRunService mit
  Coverage+Konflikt-Statistik, CascadeService REUSE-RULE-AI(stub)-HUMAN
  ohne Persistenzwirkung, REST unter /api/v1/rules, Flyway V0009).

Test-Bilanz: `./mvnw -T 1C test` -> BUILD SUCCESS. 96 Tests gruen,
11 Docker-abhaengig (geskippt ohne Docker), 0 rot. Details in
`docs/20260417/iteration-0{4,5}-*.md`.

## Arbeitsregeln (aus CLAUDE.md)
- Arbeite auf dem Branch, der dir vom System vorgegeben wird (i.d.R. ein
  `claude/...`-Branch). Merge am Ende nach main (fast-forward), nur wenn
  der User darum bittet.
- TDD: Tests zuerst, NICHT die Tests aendern, wenn sie rot werden.
- Conventional Commits auf Deutsch + Jira-Key im Footer:
    feat(workflow): ... \n\n CVM-15
- @DisplayName auf Deutsch, fachliche Strings auf Deutsch, technische
  Bezeichner Englisch.
- Persistenz-Tests nutzen Testcontainers-Postgres (pgvector/pgvector:pg16)
  und werden ueber @EnabledIf auf DockerAvailability#isAvailable
  geskippt, wenn kein Docker da ist.
- ArchUnit-Modulgrenzen (cvm-architecture-tests) sind hart. Vor allem:
  `cvm-api` darf NICHT auf `cvm-persistence` zugreifen -> fuer
  Controller-DTOs entweder `*View`-Record im Application-Modul (wie
  ProfileView/RuleView) oder Mapping ueber den Service.
- Fortschrittsberichte kommen unter
  docs/YYYYMMDD/iteration-NN-fortschritt.md, Test-Summary unter
  docs/YYYYMMDD/iteration-NN-test-summary.md, offene Punkte kumulativ
  in docs/YYYYMMDD/offene-punkte.md.
- Flyway-Migrationen nicht rueckwaerts aendern; nur vorwaerts via neue
  V00NN-Dateien. Naechste freie Nummer ist **V0010**.
- Kein Secret ins Repo.
- `@WebMvcTest` fuer einen neuen Controller-Slice braucht eine eigene
  `*TestApi`-Slice-Config im Test-Paket (siehe ProfileTestApi,
  RulesTestApi), damit nicht das globale `TestApiApplication` andere
  Controller mitzieht.

## Ungeloeste Entscheidungen / Backlog (Auszug aus offene-punkte.md)
- **Security**: Rollen `CVM_PROFILE_AUTHOR`, `CVM_PROFILE_APPROVER`,
  `CVM_RULE_AUTHOR`, `CVM_RULE_APPROVER`, `CVM_REVIEWER` noch nicht
  verdrahtet (aktuell `CVM_ADMIN` bzw. `authenticated()`).
- **Condition-JSON** liegt als TEXT, nicht JSONB. JSONB-Switch bei
  Bedarf Backlog.
- **Dry-Run**: Profil-at-point-in-time wird nicht beruecksichtigt,
  nutzt aktuelles ACTIVE-Profil.
- **Regel-Versionierung**: nur Status-Zyklus (DRAFT/ACTIVE/RETIRED).
  Immutable-Versionierung wie Assessment/Profil ist offen.
- Resilience4j (Retry/Circuit-Breaker) nachziehen, vorzugsweise parallel
  zu Iteration 11 (LLM-Gateway).
- GHSA-Token in Vault hinterlegen; SonarCloud-Keys sind Platzhalter.
- Scheduled-Jobs brauchen Cluster-Koordination (ShedLock) sobald mehr
  als eine Instanz laeuft.
- Fachkonzept v0.2 unter docs/konzept/ ist ein Platzhalter.
- `archunit.properties#failOnEmptyShould=false` ist provisorisch;
  zurueckrollen in Iteration 11.

## Naechster Schritt
Iteration 06 ist die erste Iteration, die den gesamten Workflow
verdrahtet: `06-Bewertungs-Workflow.md` im Repo-Root beschreibt den
Auftrag. Ziel:
- CascadeService wird an Persistenz angebunden (erzeugt
  PROPOSED-Assessments; REUSE erzeugt Version+1; RULE-Treffer nutzt
  `rationaleSourceFields` aus der Regel).
- Bewertungs-Queue (Repository-Query fuer offene PROPOSED +
  NEEDS_REVIEW je Umgebung).
- Approve/Reject-Endpunkte mit Vier-Augen fuer Downgrades auf
  NOT_APPLICABLE/INFORMATIONAL.
- Abhaengigkeiten: 02 (Scan-Ingestion, ScanIngestedEvent) und 05
  (CascadeService, RuleEngine).

### Arbeitsweise
1. Lies CLAUDE.md, README-Iterationsplan.md,
   docs/20260417/offene-punkte.md, docs/20260417/iteration-05-
   fortschritt.md, 06-Bewertungs-Workflow.md.
2. Erstelle/wechsle auf den vom System vorgegebenen claude/...-Branch.
3. Plane kurz in docs/YYYYMMDD/iteration-06-plan.md (YYYYMMDD = heute).
4. TDD: Tests zuerst, dann Produktionscode. Neue Tests laufen ohne
   Docker durch.
5. `./mvnw -T 1C test` -> BUILD SUCCESS als Abschlusskriterium.
6. Schreibe iteration-06-fortschritt.md und iteration-06-test-summary.md,
   erweitere offene-punkte.md.
7. Commit mit Conventional-Commit-Message + CVM-15 im Footer, dann push.
8. Frage den User, ob mit Iteration 07 weitergemacht werden soll.

Nach Iteration 06 geht es sequentiell weiter (07-21). Die Prompts liegen
je als `NN-*.md` im Repo-Root. Roadmap und Abhaengigkeiten stehen in
`README-Iterationsplan.md`.

### Hilfreiche Signposts im Repo
- Cascade-Grundgeruest: `cvm-application/cascade/CascadeService.java`.
- Assessment-Immutability + markiereAlsUeberholt:
  `cvm-persistence/src/main/java/com/ahs/cvm/persistence/assessment/`.
- AssessmentLookupService (REUSE-Lookup, speichereNeueVersion,
  markiereAlsUeberholt): `cvm-application/assessment/`.
- RuleEngine.evaluate + ProposedResult: `cvm-application/rules/`.
- Profile-Event-Listener-Muster (ref): `cvm-application/profile/
  AssessmentReviewMarker.java`.
- REST-Exception-Handler-Muster: `cvm-api/profile/
  ProfileExceptionHandler.java` und `cvm-api/rules/
  RulesExceptionHandler.java`.
- ArchUnit-Regeln: `cvm-architecture-tests/src/test/java/com/ahs/cvm/
  architecture/ModulgrenzenTest.java`.

---
