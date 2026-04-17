# Iteration 04 – Fortschrittsbericht

**Jira**: CVM-13
**Datum**: 2026-04-17
**Ziel**: Umgebungen erhalten versionierte Kontextprofile (YAML), Vier-Augen-
Freigabe und Diff-getriebenes `NEEDS_REVIEW` an Assessments.

## 1 Was wurde gebaut

### Domain (`cvm-domain`)
- Neues Enum `ProfileState` mit `DRAFT | ACTIVE | SUPERSEDED`.
- `AssessmentStatus` um `NEEDS_REVIEW` erweitert. Die im Konzept zugesicherte
  Reihenfolge (`PROPOSED` zuerst) bleibt unveraendert &mdash;
  `NEEDS_REVIEW` steht am Ende.

### Persistenz (`cvm-persistence`)
- `ContextProfile`-Entity um `state`, `proposedBy`, `supersededAt` erweitert.
- `ContextProfileRepository`: neue Query
  `findFirstByEnvironmentIdAndStateOrderByVersionNumberDesc`.
- `Assessment`-Entity um `rationaleSourceFields` (JSONB-Array) und
  `reviewTriggeredByProfileVersion` (UUID) erweitert.
- `AssessmentRepository`:
  - `findAktiveIdsByEnvironmentAndSourceFields` (Native Query mit `?|`-Operator
    gegen JSONB-Array).
  - `markiereAlsReview` als `@Modifying @Query`, setzt `status=NEEDS_REVIEW`
    + `reviewTriggeredByProfileVersion`. Umgeht bewusst den
    {@code AssessmentImmutabilityListener} &mdash; es ist eine auditierbare
    System-Transition, keine fachliche Aenderung.
- Flyway `V0008__profil_versionierung.sql`:
  - `context_profile`: neue Spalten, Check-Constraint, Partial-Unique-Index
    `(environment_id) WHERE state='ACTIVE'`.
  - `assessment`: neue Spalten, erweiterter Status-Check,
    GIN-Index auf `rationale_source_fields`.

### Anwendungsschicht (`cvm-application/profile`)
- `ProfileFieldDiff` (Record) mit Enum `ChangeType` (CREATED/REMOVED/CHANGED).
- `ProfileDiffBuilder`: deterministischer, rekursiver JsonNode-Diff; Objekte
  per `TreeSet`, Arrays per `[index]`-Suffix, Ausgabe alphabetisch sortiert.
- `ContextProfileYamlParser`: nutzt `YAMLMapper` (transitiv via
  `json-schema-validator`) und validiert gegen
  `/profile/profile-schema-v1.json`. Fehler werden feldgenau und
  deutschsprachig uebersetzt.
- `ParsedProfile` (Record) haelt den JsonNode und den YAML-Quelltext.
- `ProfileView` (Record) &mdash; Read-Model, damit `cvm-api` nicht gegen die
  ArchUnit-Regel `api -> persistence` verstoesst.
- `ContextProfileService` mit
  - `latestActiveFor`, `proposeNewVersion`, `approve`, `diff`,
    `environmentOf`.
  - Vier-Augen-Pruefung (`proposedBy` != `approverId`) &rarr;
    `FourEyesViolationException`.
  - Publikation eines `ContextProfileActivatedEvent` mit der Menge aller
    geaenderten Pfade.
- `AssessmentReviewMarker` (`@EventListener`): lauscht auf das Activated-Event
  und feuert die Batch-Update-Query.
- Exception-Typen `ProfileValidationException`, `ProfileNotFoundException`,
  `FourEyesViolationException`.

### REST (`cvm-api/profile`)
- `ProfileController`:
  - `GET /api/v1/environments/{id}/profile`
  - `PUT /api/v1/environments/{id}/profile`
  - `POST /api/v1/profiles/{versionId}/approve`
  - `GET /api/v1/profiles/{versionId}/diff?against=latest|{UUID}`
- DTOs: `ProfileResponse`, `ProfilePutRequest`, `ProfileApproveRequest`,
  `ProfileDiffEntry`.
- `ProfileExceptionHandler`: 404 bei NotFound, 400 bei Validation,
  409 bei Vier-Augen- oder State-Konflikt.

### Test-Konfiguration
- `ProfileTestApi` als slice-`@SpringBootConfiguration` im Profile-Test-Paket.
  Stellt sicher, dass `ProfileControllerWebTest` nur das Profile-Paket
  scannt und damit vom bereits existierenden Scan-Test isoliert ist.

### Tests (TDD, alle gruen)
- `ProfileDiffBuilderTest` (7): identisch, boolean-flip, created, removed,
  tiefe Struktur, Array-Index, Reihenfolge.
- `ContextProfileYamlParserTest` (5): gueltig, Pflichtfeld fehlt, falscher
  Typ, YAML-Syntaxfehler, Enum-Verstoss.
- `ContextProfileServiceTest` (5): propose, Vier-Augen-Verstoss,
  Aktivierung + Event, erste Version, diff.
- `AssessmentReviewMarkerTest` (2): markiert betroffene Assessments,
  leerer Diff triggert keinen Call.
- `ProfileControllerWebTest` (5): GET ok, GET 404, PUT 201, PUT 400
  bei Schema-Fehler, POST 409 bei Vier-Augen-Verstoss.

## 2 Was laenger dauerte

- **`@WebMvcTest` + Multi-Controller**: Der existierende
  `TestApiApplication` scannt nur `com.ahs.cvm.api.scan`. Der
  Profile-Webtest benoetigt eine eigene Slice-Config, weil sonst der
  ScanController mit ins Kontext-Laden gezogen wird und der nicht
  gemockte `ScanIngestService` den Kontextaufbau sprengt. Loesung:
  `ProfileTestApi` im Profile-Test-Paket. Spring findet die
  {@code @SpringBootConfiguration} im naeher liegenden Paket zuerst.
- **Arch-Regel `api -> persistence`**: Der erste Controller-Entwurf nutzte
  direkt `ContextProfile`. Eingefuehrt wurde `ProfileView` als
  Application-DTO, das vom Service zurueckgegeben wird.
- **`json-schema-validator` Locale**: Die Default-Nachrichten sind
  englisch. Eigene `uebersetze()`-Methode mapt die Typ-Keys
  (`required`, `type`, `enum`, `additionalProperties`, &hellip;) auf
  deutsche, feldgenaue Meldungen.

## 3 Abweichungen vom Prompt

1. **Flyway-Nummer**: Prompt nennt `V0007__profil_versionierung.sql`; V0007
   war bereits in Iteration 03 fuer die CVE-Anreicherung vergeben. Die
   Profil-Migration laeuft als **V0008**. Gleicher Inhalt.
2. **`NEEDS_REVIEW` in Assessment-Enum**: Der Prompt impliziert den Wert
   durch das SQL-Beispiel `SET status='NEEDS_REVIEW'`. Ich habe ihn explizit
   in `AssessmentStatus` und in den DB-Check aufgenommen.
3. **Immutability-Listener-Bypass**: Der Listener blockiert jeden
   JPA-Update auf Assessment ausser `markiereAlsUeberholt`. Um ohne
   neue Carve-Outs am Listener zu arbeiten, laeuft der Review-Batch via
   `@Modifying`-Query, was den Listener garantiert umgeht. Ist ausserdem
   semantisch korrekt (keine fachliche Aenderung). Der Ausloeser wird in
   der neuen Spalte `review_triggered_by_profile_version` festgehalten.
4. **`rationaleSourceFields`**: Der Prompt listet das Feld als "neu in
   Assessment". Ich habe es bereits hier persistiert, auch wenn die Regel-
   Engine (Iteration 05) und die KI-Services (Iteration 13) es erst
   nachtraeglich befuellen. Ohne das Feld laesst sich der
   Event-Listener nicht sinnvoll testen.

## 4 Entscheidungen fuer Sebastian

- `ProfileState`: DRAFT/ACTIVE/SUPERSEDED reicht. Soll es zusaetzlich einen
  `REJECTED`-Status geben (Draft verworfen)? Aktuell nicht geplant.
- Profil-Schema v1: welche Felder sollen in Iteration 05 (Regel-Engine)
  konsumiert werden? Vorschlag: zumindest `architecture.*` und
  `network.*` fuer die Treffer-Mapping-Regeln.
- Wer darf Profile anlegen/freigeben? Vorschlag:
  Rolle `CVM_PROFILE_AUTHOR` fuer PUT, `CVM_PROFILE_APPROVER` fuer POST.
  Noch nicht an den Endpunkten verdrahtet &mdash; heute liegt das unter
  dem Default-`authenticated()`-Gate der `WebSecurityConfig`.

## 5 Naechster Schritt

**Iteration 05 &mdash; Regel-Engine** (CVM-14). Deterministische
Cascade-Stufe RULE nutzt die jetzt verfuegbaren Profil-Werte.

## 6 Build-Status

```
./mvnw -T 1C test  BUILD SUCCESS
```

- cvm-domain `EnumTest`: 4/4
- cvm-persistence ITs (Docker): 6 geskippt
- cvm-application:
  - `ProfileDiffBuilderTest`: 7/7
  - `ContextProfileYamlParserTest`: 5/5
  - `ContextProfileServiceTest`: 5/5
  - `AssessmentReviewMarkerTest`: 2/2
  - `CycloneDxParserTest`: 4/4
  - `SbomEncryptionTest`: 3/3
  - `CveEnrichmentServiceTest`: 2/2
- cvm-integration WireMock-Tests: 8/8
- cvm-api:
  - `ProfileControllerWebTest`: 5/5
  - `ScanControllerWebTest`: 5/5
- cvm-app ITs (Docker): 5 geskippt
- cvm-architecture `ModulgrenzenTest`: 7/7

Gesamt: **68 Tests, 57 gruen, 11 geskippt ohne Docker, 0 rot**.

---

*Autor: Claude Code.*
