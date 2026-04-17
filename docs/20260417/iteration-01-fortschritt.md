# Iteration 01 – Fortschrittsbericht

**Jira**: CVM-10
**Datum**: 2026-04-17
**Ziel**: Fachliche Kern-Entities und Flyway-Baseline fuer ab Iteration 02
benoetigte Scan-Ingestion.

## 1 Was wurde gebaut

### Enums (`cvm-domain/enums`)
- `AhsSeverity` (CRITICAL-NOT_APPLICABLE)
- `AssessmentStatus` (PROPOSED-APPROVED-REJECTED-SUPERSEDED)
- `ProposalSource` (REUSE-RULE-AI_SUGGESTION-HUMAN)
- `MitigationStrategy`, `MitigationStatus`, `EnvironmentStage`

### Flyway-Migrationen
- `V0001__produkt_und_version.sql` &mdash; `product`, `product_version`
- `V0002__umgebung_und_profil.sql` &mdash; `environment` (mit Stage-Check),
  `context_profile` (versioniert), `environment_deployment`
- `V0003__scan_und_komponente.sql` &mdash; `scan`, `component` (Unique-PURL),
  `component_occurrence`
- `V0004__cve_und_finding.sql` &mdash; `cve` (inkl. KEV/EPSS), `finding`
- `V0005__assessment_und_mitigation.sql` &mdash; `assessment` (inkl.
  `ai_suggestion_id` ohne FK als Platzhalter fuer Iteration 11),
  `mitigation_plan`

### JPA-Entities und Repositories (`cvm-persistence`)
- `Product` / `ProductRepository`, `ProductVersion` /
  `ProductVersionRepository`
- `Environment` / `EnvironmentRepository`, `EnvironmentDeployment` /
  `EnvironmentDeploymentRepository`
- `ContextProfile` / `ContextProfileRepository`
- `Scan`, `Component`, `ComponentOccurrence` (+ Repositories)
- `Cve` / `CveRepository`
- `Finding` / `FindingRepository`
- `Assessment` mit `AssessmentImmutabilityListener` und
  `ImmutabilityException`; `AssessmentRepository`
- `MitigationPlan` / `MitigationPlanRepository`
- `PersistenceConfig` aktiviert Entity-/Repository-Scan

Alle IDs sind `UUID`, clientseitig via `@PrePersist` gesetzt. Alle
Zeitstempel sind `Instant` auf `TIMESTAMPTZ`. `@PrePersist` setzt
`createdAt`, `@PreUpdate` `updatedAt`. Die Assessment-Felder, die nicht
versioniert werden duerfen (severity, proposalSource, rationale, finding,
productVersion, environment, cve, version, aiSuggestionId, decidedBy,
decidedAt), tragen `updatable = false`.

### Immutability-Mechanik
- `AssessmentImmutabilityListener#@PreUpdate` wirft
  `ImmutabilityException`, wenn das transient-Flag
  `supersedingAllowed` nicht gesetzt ist.
- Einziger legitimer Update-Pfad ist
  `Assessment#markiereAlsUeberholt(...)` (setzt Flag, `supersededAt`,
  Status `SUPERSEDED`).
- `AssessmentLookupService#speichereNeueVersion(...)` blockiert ueber
  `existsById` zusaetzlich doppelte Persist-Versuche.

### Domain-Services (`cvm-application`)
- `ProductCatalogService` &mdash; Lookup und Persistenz Produkt/Version
- `EnvironmentService` &mdash; Umgebung und Deployments
- `AssessmentLookupService` &mdash; aktuelles Assessment, Historie,
  aktive Freigaben, Supersede

### Tests
- `EnumTest` in `cvm-domain` (4/4 gruen) &mdash; sichert Enum-Reihenfolge.
- `AbstractPersistenceIntegrationsTest` + `support.DockerAvailability`
  als Skip-Gate fuer Umgebungen ohne Docker.
- `ProductRepositoryIntegrationsTest` (2 Tests, geskippt ohne Docker).
- `AssessmentImmutableTest` (2 Tests, geskippt ohne Docker).
- `FlywayMigrationReihenfolgeTest` (2 Tests, geskippt ohne Docker).
- `ModulgrenzenTest` (ArchUnit, 7/7 gruen).

### Smoke/Flyway-ITs aus Iteration 00
Weiter vorhanden; 3 Testfaelle werden ohne Docker geskippt.

## 2 Was ist laenger als erwartet

- **EntityListeners mit Spring Data saveAndFlush**: Der Listener muss
  zwingend `@EntityListeners` an der Klasse tragen, nicht am Feld. Auch
  der Zugriff auf `@Transient`-Felder innerhalb des `@PreUpdate` ist
  robust, solange die Instanz im gleichen Persistence-Context bleibt.

## 3 Abweichungen vom Prompt

1. **PersistenceConfig**: zusaetzlich angelegt, um `@EnableJpaRepositories`
   und `@EntityScan` explizit an das Basispackage `com.ahs.cvm.persistence`
   zu binden. Spring Boot wuerde das normalerweise ueber das App-Package
   `com.ahs.cvm.app` nicht sehen.
2. **ai_suggestion_id**: Als nullable UUID ohne FK angelegt. Iteration 11
   ergaenzt die Zieltabelle und FK.
3. **`ContextProfile.version_number`** heisst nicht `version`, weil
   `version` in Kombination mit `@Version` (JPA-Optimistic-Locking)
   Nebenwirkungen haette. Konsistenter Spalten-/Feldname.
4. **`vector`-Extension** statt `pgvector` (Name-Korrektur aus
   Iteration 00 durchgezogen).

## 4 Entscheidungen, die Sebastian bestaetigen muss

- Soll das `updatable=false` auf den meisten Assessment-Spalten auch
  formal per JPA-Konstruktor-Garantie abgesichert werden (Value-Object
  aus dem Builder, immutable nach Persist)?
- `flex-layout` ist im Frontend nicht zwingend noetig (Material 18
  unterstuetzt CSS-Grid nativ). Entfernen oder belassen?

## 5 Vorgeschlagener naechster Schritt

**Iteration 02 &mdash; SBOM-Ingestion** (CVM-11). Abhaengigkeit 01 erfuellt.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- `EnumTest`: 4/4 gruen
- Persistence-ITs (Testcontainers): 6 Tests, 6 geskippt ohne Docker
- `FlywayBaselineTest`/`SmokeIntegrationTest` aus Iteration 00: 3
  geskippt ohne Docker
- `ModulgrenzenTest` (ArchUnit): 7/7 gruen

Gesamt: 20 Testfaelle, 11 gruen, 9 geskippt (Docker-abhaengig), 0 rot.

---

*Autor: Claude Code.*
