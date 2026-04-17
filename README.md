# CVE-Relevance-Manager (CVM)

Relevanzbewertung von CVEs gegen Produkte, ProduktVersionen und Umgebungen –
entwickelt von adesso health solutions GmbH als Nachbau-/Weiterentwicklungs-
pendant zu PortalCore und ZPV.

> Verbindliche Arbeitsgrundlage ist `CLAUDE.md`. Die Iterations-Roadmap steht
> in `README-Iterationsplan.md`. Das Fachkonzept liegt unter
> `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md`.

## Voraussetzungen

| Werkzeug | Mindestversion |
|---|---|
| JDK        | Temurin 21 |
| Maven      | 3.9+ (ein Maven-Wrapper `./mvnw` liegt bei) |
| Node.js    | 20 LTS |
| Docker     | 24+ (wird fuer Testcontainers und Docker-Compose benoetigt) |

## Quickstart

```bash
# 1. Lokale Infrastruktur hochfahren (Postgres + pgvector, Keycloak, MailHog)
docker compose up -d

# 2. Backend bauen und starten
./mvnw -T 1C clean verify
./mvnw spring-boot:run -pl cvm-app

# 3. Frontend starten
cd cvm-frontend
npm install
npm start
```

- API:        http://localhost:8081/actuator/health
- OpenAPI:    http://localhost:8081/swagger-ui.html
- Frontend:   http://localhost:4200
- Keycloak:   http://localhost:8080 (Admin: admin/admin)
- MailHog UI: http://localhost:8025

## Module

```
cvm-domain/              POJOs, Value Objects (keine Spring-Abhaengigkeiten)
cvm-persistence/         JPA-Entities, Repositories, Flyway-Migrationen
cvm-application/         Services, Use Cases, Orchestrierung
cvm-integration/         Adapter zu NVD/GHSA/KEV/EPSS, Jira, Git, SMTP
cvm-llm-gateway/         LLM-Abstraktion (leer bis Iteration 11)
cvm-ai-services/         KI-Anwendungsfaelle (leer bis Iteration 13)
cvm-api/                 REST-Controller, DTOs, OpenAPI
cvm-app/                 Spring-Boot-Einstiegspunkt
cvm-architecture-tests/  ArchUnit-Regeln (Modulgrenzen)
cvm-frontend/            Angular 18 (Standalone, Material, Tailwind)
```

## Tests

```bash
./mvnw -T 1C test                 # Unit- + ArchUnit-Tests
./mvnw -T 1C verify               # inkl. Testcontainers-IT (Docker erforderlich)
cd cvm-frontend && npm test       # Angular Unit-Tests
```

Integrationstests, die einen Postgres-Container benoetigen, werden automatisch
uebersprungen, wenn kein Docker-Daemon erreichbar ist
(siehe `com.ahs.cvm.app.DockerAvailability`).

## Weitere Dokumente

- `CLAUDE.md` &mdash; Arbeitsregeln und Tech-Stack (verbindlich)
- `README-Iterationsplan.md` &mdash; Phasen 0 bis 4 der Umsetzung
- `docs/konzept/` &mdash; Fachkonzept v0.2
- `docs/YYYYMMDD/` &mdash; tagesaktuelle Fortschrittsberichte

## Lizenz

Interne Entwicklung der adesso health solutions GmbH.
