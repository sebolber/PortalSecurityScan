# Iteration 00 – Fortschrittsbericht

**Jira**: CVM-1
**Datum**: 2026-04-17
**Ziel**: Leeres Repository in arbeitsfaehiges Multi-Modul-Projekt ueberfuehren.

## 1 Was wurde gebaut

### Repository-Fundament
- `.claudeignore` und Platzhalter-Fachkonzept unter `docs/konzept/`
- `docs/iterationen/.gitkeep`
- `README.md` mit Quickstart, Modulueberblick, Test-Hinweisen
- `docs/20260417/` als Zielordner fuer diesen Bericht (und spaetere Iterationen)

### Maven-Multi-Modul
- Eltern-`pom.xml` mit Spring-Boot-BOM 3.3.5, Java 21, JaCoCo (80 %-Schwelle),
  Spotless, Checkstyle, Pitest, Surefire, Failsafe
- Modul-POMs:
  - `cvm-domain` &mdash; reine POJOs, nur Lombok und JUnit
  - `cvm-persistence` &mdash; Spring Data JPA, Flyway, Postgres-Treiber, pgvector-JDBC
  - `cvm-application` &mdash; Anwendungslogik
  - `cvm-integration` &mdash; WebFlux, Mail, CycloneDX-Core
  - `cvm-llm-gateway` &mdash; leer, vorbereitet fuer Iteration 11
  - `cvm-ai-services` &mdash; leer, vorbereitet fuer Iteration 13
  - `cvm-api` &mdash; Web, Security, SpringDoc, MapStruct, Validation
  - `cvm-app` &mdash; Spring-Boot-Main + Actuator
  - `cvm-architecture-tests` &mdash; ArchUnit
- Maven-Wrapper `./mvnw` (Maven 3.9.9)

### Laufzeit und Konfiguration
- `CvmApplication.java` mit `scanBasePackages = "com.ahs.cvm"`
- `application.yaml` mit Platzhaltern `${VAR:default}` fuer Datasource,
  OAuth2-Issuer, Mail, Server-Port
- `WebSecurityConfig` (JWT-Resource-Server, Actuator + OpenAPI ohne Auth)
- `OpenApiConfig` mit Titel/Version
- Flyway-Baseline `V0000__baseline.sql` &mdash; aktiviert `uuid-ossp` und
  `vector`-Extension, legt `audit_trail` an

### Infrastruktur lokal
- `docker-compose.yml` mit PostgreSQL 16 (`pgvector/pgvector:pg16`),
  Keycloak 24 (start-dev, Realm-Import), MailHog
- `infra/keycloak/dev-realm.json` mit Test-Client `cvm-local`, Rollen
  `CVM_VIEWER|ASSESSOR|APPROVER|ADMIN` und Test-Usern

### Testsaeulen
- `AbstractIntegrationTest` mit Testcontainers-PostgreSQL (Reuse=true)
- `DockerAvailability` &mdash; robuste Pruefung auf `DOCKER_HOST` oder
  `/var/run/docker.sock`, damit Tests ohne Docker sauber geskippt werden
- `SmokeIntegrationTest` &mdash; Actuator-Health auf `UP`
- `FlywayBaselineTest` &mdash; prueft Extensions und `audit_trail`-Tabelle
- `ModulgrenzenTest` (ArchUnit) &mdash; 7 Regeln: Domain ohne Spring,
  Persistence nur auf Domain, API nicht direkt auf Persistence, LLM-Gateway
  isoliert, Application/Integration kennen API nicht
- `archunit.properties` mit `archRule.failOnEmptyShould=false` fuer die
  Bootstrap-Phase (wird in Iteration 01 entfernt, sobald die Module
  tatsaechlich Klassen tragen)

### Frontend-Shell
- Angular-18-Scaffold in `cvm-frontend/` (Standalone, Routing, SCSS)
- `package.json` mit Angular Material, Tailwind, PostCSS, ESLint,
  keycloak-angular/keycloak-js, ngx-echarts, ngx-monaco-editor-v2
- `angular.json` mit Build/Serve/Test/Lint-Targets, SelectorPrefix `cvm`
- `ShellComponent` mit Header und Sidebar (Dashboard aktiv, restliche
  Menueintraege disabled)
- `DashboardComponent` (Platzhalter), `LoginCallbackComponent`
- `AppConfigService` laedt `assets/config.json`
- `AppComponent` + Karma-Spec
- Tailwind mit adesso-Farbpalette

### CI
- `.gitlab-ci.yml` mit Stages `validate`, `build`, `test`, `sonar`, `package`
- Backend-Tests im Test-Stage mit Docker-in-Docker fuer Testcontainers
- Sonar nur auf `main` und Release-Branches, `allow_failure: true` bis
  Sonar-Account verbunden ist

## 2 Was ist laenger als erwartet

- **ArchUnit vs. leere Module**: Neue Default-Regel `failOnEmptyShould=true`
  bricht bei unbesetzten Paketen. Loesung: `archunit.properties` in
  `cvm-architecture-tests/src/test/resources`. Muss in Iteration 01
  zurueckgenommen werden.
- **Testcontainers ohne Docker**: Mit reinem
  `@Testcontainers(disabledWithoutDocker = true)` kam der Spring-Kontext
  trotzdem hoch, weil nur `@Container`-Felder geskippt werden. Loesung:
  eigener `DockerAvailability`-Helper plus `@EnabledIf` direkt auf den
  Subklassen (JUnit 5 erbt `@EnabledIf` nicht ueber abstrakte Basisklassen).

## 3 Abweichungen vom Prompt (mit Begruendung)

1. **Extension-Name**: Der Prompt nennt `CREATE EXTENSION IF NOT EXISTS
   "pgvector"`. Der offizielle Extension-Name des pgvector-Projekts ist
   `vector`. Die Baseline verwendet daher `"vector"`.
2. **Eltern-POM erbt von `spring-boot-starter-parent`** statt nur das BOM
   zu importieren. Das reduziert Plugin-Boilerplate. Alternativ kann mit
   `dependencyManagement`-Import umgestellt werden, falls ein eigenes
   Release-Lifecycle benoetigt wird.
3. **`cvm-architecture-tests`** ist als **eigenes Modul** angelegt (nicht
   als Test-Source in `cvm-app`), damit ArchUnit alle Klassen aller Module
   ueber deren Compile-Ausgabe sieht, sobald sie existieren.
4. **Angular-Dependencies nicht per `npm install` gezogen**. Die
   `package.json`, `angular.json` und TypeScript-Configs sind vorhanden;
   das Frontend ist damit direkt per `npm install && npm start` startklar,
   aber in dieser Iteration wurde kein `node_modules/` gebaut. `ng new`
   wuerde hier nur das gleiche Ergebnis produzieren und laeuft spaeter in
   CI.
5. **Container-Build** ist in der CI als Stub angelegt (Iteration 00 soll
   laut Prompt keinen Push erzeugen). Echtes Container-Building kommt in
   Iteration 21.

## 4 Entscheidungen, die Sebastian bestaetigen muss

- `sonar.projectKey` und `sonar.organization` sind Platzhalter
  (`TBD`). Sobald SonarCloud-Organisation angelegt ist, eintragen.
- PostgreSQL-Extension heisst `vector`, nicht `pgvector` &mdash; Prompt oben
  entsprechend korrigieren?
- Keycloak-Realm-Datei ist minimal. Fuer ein produktives Setup folgen
  Gruppen, Client-Scopes, Rollen-Hierarchie in Iteration 07.

## 5 Vorgeschlagener naechster Schritt

**Iteration 01 &mdash; Domain-Kern**
(`01-Domain-Kern.md`, Jira CVM-10). Abhaengigkeit 00 erfuellt. Zu Beginn
`archunit.properties` zuruecknehmen (`failOnEmptyShould=true`), sobald
die ersten Domain-Klassen da sind.

## 6 Build-Status

```
./mvnw -T 1C test                 BUILD SUCCESS
./mvnw -T 1C -DskipTests package  BUILD SUCCESS
```

Ergebnis der Tests:
- `ModulgrenzenTest`: 7/7 gruen
- `SmokeIntegrationTest`: 1/1 geskippt (kein Docker in der Sandbox)
- `FlywayBaselineTest`: 2/2 geskippt (kein Docker in der Sandbox)

Die Integrationstests laufen lokal oder in CI mit `docker compose up -d`.

---

*Autor: Claude Code (Opus 4.7, 1M-Context). Review durch Sebastian Bolber.*
