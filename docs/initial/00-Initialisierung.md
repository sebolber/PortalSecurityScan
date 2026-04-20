# Iteration 00 – Initialisierung des CVE-Relevance-Manager

**Jira**: CVM-1
**Ziel**: Leeres Git-Repository in ein arbeitsfähiges Multi-Modul-Projekt überführen,
sodass ab Iteration 01 fachlich entwickelt werden kann.

---

## 0 Kontext

Du bist Claude Code. Du arbeitest in einem **leeren Git-Repository** für das Projekt
`cvm` (CVE-Relevance-Manager) der adesso health solutions GmbH. Das Fachkonzept liegt
unter `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md`. Wenn es noch nicht
existiert, gehe davon aus, dass es parallel bereitgestellt wird; erzeuge einen
Platzhalter.

In dieser Iteration wird **kein Fachcode** geschrieben. Du legst die Grundlage.

---

## 1 Voraussetzungen prüfen (und ergänzen falls nötig)

Lege diese Dateien **zuerst** im Repository-Root an (Inhalte siehe Anhänge):
- `CLAUDE.md` — Anhang A
- `.claudeignore` — Anhang B
- `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md` — Platzhalter, falls nicht
  vorhanden
- `docs/iterationen/` — Ordner für Folgeprompts (leer anlegen, `.gitkeep`)

Nach dem Anlegen dieser Dateien liest du `CLAUDE.md` und folgst ab hier strikt
den dort definierten Regeln.

---

## 2 Scope dieser Iteration

### IN Scope
1. Maven-Multi-Modul-Projekt gemäß `CLAUDE.md` Abschnitt 3 (Ordnerstruktur).
2. Eltern-POM mit Dependency- und Plugin-Management (Java 21, Spring Boot
   3.3.x BOM, Testcontainers, Lombok, MapStruct, ArchUnit, Spotless,
   Checkstyle, JaCoCo, Pitest, SonarCloud-Config).
3. Modul-POMs mit minimaler Abhängigkeitsliste (jedes Modul compiliert,
   enthält aber bewusst wenig).
4. Spring-Boot-Hauptanwendung in `cvm-app` (nur Startklasse + `application.yaml`
   mit Platzhaltern).
5. Flyway-Baseline: `cvm-persistence/src/main/resources/db/migration/V0000__baseline.sql`
   mit Extension-Aktivierung (`pgvector`, `uuid-ossp`) und Setup-Audit-Tabelle.
6. Docker-Compose für lokale Entwicklung: PostgreSQL 16 mit pgvector,
   Keycloak (Entwicklungsmodus), MailHog (lokaler SMTP-Fake).
7. Testcontainers-Setup für Integrationstests (ReusableContainer, pgvector-Image).
8. ArchUnit-Grundgerüst: Modulgrenzen-Tests gemäß CLAUDE.md Abschnitt 3.
9. Angular-18-Scaffold in `cvm-frontend/` mit Standalone-API, Keycloak-Stub,
   Layout-Skeleton (Header/Sidebar/Content), Routing-Grundgerüst.
10. CI-Pipeline-Vorlage `.gitlab-ci.yml` mit Stages: `validate`, `build`,
    `test`, `sonar`, `package`.
11. Pre-Commit-Konfiguration (Spotless, Checkstyle, ESLint/Prettier für
    Angular).
12. README.md im Root mit Quickstart, Modulübersicht, Link auf Konzept.
13. OpenAPI-Grundkonfiguration (SpringDoc) in `cvm-api`, Health- und
    Info-Endpunkte aktiv.

### NICHT IN Scope
- Fachliche Entities, Services, Controller.
- Echte REST-Endpunkte außer `/actuator/health`, `/actuator/info`, `/v3/api-docs`.
- LLM-Gateway-Modul wird nur **angelegt (leeres Modul)**, nicht implementiert.
- Frontend-Inhalte außer Shell und Login-Redirect.
- Produktions-Keycloak-Konfiguration.

---

## 3 Aufgaben

### 3.1 Struktur anlegen
Erzeuge die Ordner-/Dateistruktur aus `CLAUDE.md` Abschnitt 3. Jede
Modul-POM verweist auf das Eltern-POM und deklariert nur die Abhängigkeiten,
die das Modul tatsächlich braucht (auch wenn es in dieser Iteration leer ist).

### 3.2 Eltern-POM
- `<java.version>21</java.version>`
- Spring Boot BOM 3.3.x (aktuellste Patch-Version)
- Globale Plugin-Versionen: spotless-maven-plugin, checkstyle,
  maven-surefire, maven-failsafe, jacoco-maven-plugin, pitest-maven
- SonarCloud-Properties (`sonar.projectKey`, `sonar.organization` als
  Platzhalter `<TBD>`)
- Quality Gate: JaCoCo schlägt fehl bei < 80 % (vorerst auf Gesamt, wird
  pro Modul verschärft)

### 3.3 Flyway-Baseline
`V0000__baseline.sql` aktiviert:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgvector";

CREATE TABLE audit_trail (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    payload JSONB
);
CREATE INDEX idx_audit_trail_entity ON audit_trail (entity_type, entity_id);
```

### 3.4 Docker-Compose
`docker-compose.yml` mit PostgreSQL 16 (Image `pgvector/pgvector:pg16`),
Keycloak (Image `quay.io/keycloak/keycloak:24.x` im `start-dev`-Modus),
MailHog (`mailhog/mailhog:latest`). Ports: 5432, 8080, 8025.

Ein Realm-Import `dev-realm.json` mit einem Test-Client `cvm-local` und
den Rollen aus Konzept v0.2 Abschnitt 7 ist vorbereitet, aber
Platzhalter-Inhalt reicht.

### 3.5 Testcontainers-Setup
Shared-Base-Test-Klasse `AbstractIntegrationTest` in `cvm-app` (oder
`cvm-persistence`), die einen statischen `PostgreSQLContainer`
(`pgvector/pgvector:pg16`) hochfährt. Testcontainers-Image-Wiederverwendung
aktivieren (`.withReuse(true)`).

### 3.6 ArchUnit-Grundgerüst
In einem Modul `cvm-architecture-tests` (oder als Test-Source in `cvm-app`)
ein ArchUnit-Test `ModulgrenzenTest`, der folgende Regeln abbildet:

```java
@AnalyzeClasses(packages = "com.ahs.cvm")
class ModulgrenzenTest {
    @ArchTest
    static final ArchRule domain_hat_keine_abhaengigkeiten =
        classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "jakarta..", "lombok..");

    @ArchTest
    static final ArchRule api_greift_nicht_direkt_auf_persistence_zu =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..");

    // weitere Regeln gemaess CLAUDE.md Abschnitt 3
}
```

### 3.7 Angular-18-Scaffold
- `ng new cvm-frontend --standalone --routing --style=scss` (ohne server-side
  rendering)
- `@angular/material` + `@angular/cdk` + `@angular/flex-layout` einbinden
- `keycloak-angular` einbinden, `AppConfigService` lädt `assets/config.json`
- Routes: `/dashboard` (Platzhalter-Komponente), `/login-callback`
- Basic Layout: `<app-shell>` mit Sidebar (Menüeinträge deaktiviert außer
  Dashboard) und Header
- Tailwind in den Build einbinden (`tailwind.config.js`, `postcss.config.js`)
- `package.json`-Skripte: `start`, `build`, `test`, `lint`, `format`

### 3.8 CI-Pipeline
`.gitlab-ci.yml` mit:
- `validate`: Spotless-Check, Checkstyle, ESLint, OpenAPI-Lint
- `build`: Maven compile, Angular build
- `test`: Maven test (mit Testcontainers), Angular test
- `sonar`: Analyse (nur auf main-Branches)
- `package`: Container-Image bauen (ohne Push in dieser Iteration)

### 3.9 README.md
Ein knappes README im Root mit:
- Kurzbeschreibung
- Voraussetzungen (Java 21, Node 20, Docker)
- Quickstart: `docker-compose up -d`, `./mvnw spring-boot:run -pl cvm-app`,
  `cd cvm-frontend && npm start`
- Verweis auf `CLAUDE.md` und Konzept

---

## 4 Tests in dieser Iteration

Da kein Fachcode geschrieben wird, testest du die **Infrastruktur**:

1. `ModulgrenzenTest` läuft grün (triviale Regeln, da noch kein Code in den
   Modulen).
2. Ein `SmokeIntegrationTest` in `cvm-app`, der den Spring-Context startet,
   Testcontainers hochfährt und `GET /actuator/health` mit Status `UP`
   erwartet.
3. Ein Angular-Smoke-Test: `AppComponent` lädt, Header ist sichtbar.
4. Ein `FlywayBaselineTest`, der via Testcontainers die Migration ausführt
   und die Extensions `pgvector` und `uuid-ossp` in der DB findet.

**Alle vier Tests müssen grün sein, bevor diese Iteration committed wird.**

---

## 5 Definition of Done

- [ ] `CLAUDE.md` und `.claudeignore` im Repo-Root.
- [ ] `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md` vorhanden (ggf. Platzhalter).
- [ ] `docs/iterationen/.gitkeep`.
- [ ] Maven-Multi-Modul compiliert: `./mvnw -T 1C clean verify` ohne Fehler.
- [ ] `docker-compose up -d` startet alle drei Services; Postgres, Keycloak,
      MailHog sind erreichbar.
- [ ] Spring-Boot-App startet lokal gegen Docker-Compose-Postgres,
      `/actuator/health` = `UP`.
- [ ] Angular-App startet mit `npm start`, Shell ist sichtbar, Dashboard-Route
      erreichbar (leere Platzhalterseite).
- [ ] Alle vier Tests aus Abschnitt 4 grün.
- [ ] ArchUnit-Tests grün.
- [ ] CI-Pipeline (lokal simulierbar mit `gitlab-runner exec docker build`)
      läuft ohne Fehler.
- [ ] Fortschrittsbericht unter `docs/YYYYMMDD/iteration-00-fortschritt.md`
      geschrieben.
- [ ] Commit-Vorschlag erzeugt:
      `chore(init): Projektstruktur und Werkzeuge initialisiert\n\nCVM-1`
- [ ] **Kein `git push`**. Commit lokal vorbereiten, Sebastian review-t
      und pusht selbst.

---

## 6 TDD-Hinweise

- **Ändere NICHT die Tests**, falls einer rot wird. Behebe den
  Produktionscode. Falls der Test selbst falsch ist, halte das in einem
  separaten Commit fest mit Begründung im Commit-Body.
- Die vier Smoke-Tests dieser Iteration sind dein Sicherheitsnetz für alle
  Folge-Iterationen. Nimm sie ernst.

---

## 7 Abschlussreport

Schreibe nach Abschluss unter `docs/YYYYMMDD/iteration-00-fortschritt.md`:

1. Was wurde gebaut (knappe Liste)
2. Was hat länger gedauert oder überraschte
3. Abweichungen von diesem Prompt (mit Begründung)
4. Welche Entscheidungen brauchen Sebastians Bestätigung
5. Vorgeschlagener nächster Schritt (sollte Iteration 01 sein)

---

## Anhang A – `CLAUDE.md`

Siehe separate Datei `repo-dateien/CLAUDE.md` im Prompt-Lieferpaket. Inhalt 1:1
übernehmen.

## Anhang B – `.claudeignore`

Siehe separate Datei `repo-dateien/.claudeignore` im Prompt-Lieferpaket. Inhalt
1:1 übernehmen.

---

**Hinweis an Claude Code**: Dies ist die Grundlagen-Iteration. Fehler hier
wirken sich auf alle folgenden Iterationen aus. Arbeite gründlich. Im Zweifel
lieber eine Rückfrage an Sebastian als eine schnelle Entscheidung, die später
teuer wird.
