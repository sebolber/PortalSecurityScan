# Iteration 23 - Rollen-Verdrahtung - Fortschritt

**Jira**: CVM-54
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Abgeschlossen**: 2026-04-18

## Umgesetzt

### 1. Keycloak-Realm erweitert

`infra/keycloak/dev-realm.json` enthaelt jetzt 11 Realm-Rollen
(vorher 4):

| Rolle                    | Zweck                                           |
|--------------------------|-------------------------------------------------|
| `CVM_VIEWER`             | Lesezugriff auf Bewertungen und Reports         |
| `CVM_ASSESSOR`           | Bewertungen vorschlagen, Queue-Bearbeitung      |
| `CVM_REVIEWER`           | Review-Kommentare, Ablehnungen                  |
| `CVM_APPROVER`           | Vier-Augen-Freigabe von Bewertungen             |
| `CVM_PROFILE_AUTHOR`     | Kontextprofile als Draft pflegen                |
| `CVM_PROFILE_APPROVER`   | Kontextprofile freigeben                        |
| `CVM_RULE_AUTHOR`        | Regeln als Draft pflegen                        |
| `CVM_RULE_APPROVER`      | Regeln aktivieren + Dry-Run                     |
| `CVM_REPORTER`           | Hardening-/Executive-Reports erzeugen           |
| `AI_AUDITOR`             | Lesezugriff auf `ai_call_audit`                 |
| `CVM_ADMIN`              | Profil-, Regel-, LLM- und Tenant-Verwaltung     |

Dev-User-Defaults:

- `t.tester@ahs.test` -> `CVM_VIEWER, CVM_ASSESSOR, CVM_REVIEWER`
- `j.meyer@ahs.test` -> `CVM_PROFILE_AUTHOR, CVM_RULE_AUTHOR,
  CVM_REPORTER, CVM_REVIEWER`
- `a.admin@ahs.test` -> `CVM_ADMIN, CVM_APPROVER,
  CVM_PROFILE_AUTHOR, CVM_PROFILE_APPROVER, CVM_RULE_AUTHOR,
  CVM_RULE_APPROVER, CVM_REPORTER, AI_AUDITOR`.

### 2. JWT-Rollen-Extraktion

Neuer `KeycloakJwtAuthoritiesConverter` in
`cvm-api/config`:

- liest `realm_access.roles` UND `resource_access.*.roles`,
- normalisiert auf `UPPERCASE`,
- legt je Rolle zwei Authorities ab:
  `CVM_ADMIN` (fuer `hasAuthority(...)`) und
  `ROLE_CVM_ADMIN` (fuer `hasRole(...)`),
- behaelt zusaetzlich die `SCOPE_*`-Authorities des
  Default-Converters (Resource-Server-Standard).

In `WebSecurityConfig` verdrahtet:

```java
http.oauth2ResourceServer(oauth ->
        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                jwtAuthenticationConverter())));
```

### 3. `@PreAuthorize` an den Fach-Endpunkten

| Endpunkt                                          | Rollen                                    |
|---------------------------------------------------|-------------------------------------------|
| `PUT /environments/{id}/profile`                  | `CVM_PROFILE_AUTHOR`, `CVM_ADMIN`         |
| `POST /profiles/{id}/approve`                     | `CVM_PROFILE_APPROVER`, `CVM_ADMIN`       |
| `POST /rules`                                     | `CVM_RULE_AUTHOR`, `CVM_ADMIN`            |
| `POST /rules/{id}/activate`                       | `CVM_RULE_APPROVER`, `CVM_ADMIN`          |
| `POST /rules/{id}/dry-run`                        | `CVM_RULE_AUTHOR`/`APPROVER`, `CVM_ADMIN` |
| `POST /assessments`                               | `CVM_ASSESSOR`, `CVM_ADMIN`               |
| `POST /assessments/{id}/approve`                  | `CVM_APPROVER`, `CVM_ADMIN`               |
| `POST /assessments/{id}/reject`                   | `CVM_APPROVER`, `CVM_REVIEWER`, `CVM_ADMIN` |
| `POST /reports/hardening`                         | `CVM_REPORTER`, `CVM_ADMIN`               |
| `GET /reports/**`                                 | `CVM_VIEWER`, `CVM_REPORTER`, `CVM_ADMIN` |
| `GET /ai/audits`                                  | `AI_AUDITOR`, `CVM_ADMIN` (unveraendert)  |
| `GET|POST /rules/suggestions`, `/rag/...`,        | `CVM_ADMIN` (unveraendert)                |
| `/admin/...`, `/alerts/config`                    |                                           |

## Sicherheits-Invarianten

- **Vier-Augen bleibt erhalten**: das Reject auf einem
  `NOT_APPLICABLE`-Downgrade erfordert weiterhin einen
  anderen User als der Antragssteller. Rolle ist der
  Gate, Identity-Vergleich der zweite Layer.
- **Admin-Superset**: `CVM_ADMIN` ist ueberall zusaetzlich
  zur Fachrolle erlaubt, damit der Admin in einem
  Produktiv-Incident nicht blockiert ist.
- **Keine Abschwaechung**: Vorher hatte `CVM_ADMIN` an den
  Rule-Endpunkten ein hartes `hasRole('CVM_ADMIN')`.
  Jetzt `hasAnyAuthority('CVM_RULE_AUTHOR|APPROVER','CVM_ADMIN')`.
  Da der Realm `ROLE_CVM_ADMIN` und `CVM_ADMIN` gleichzeitig
  traegt, bleibt der Admin weiterhin berechtigt.

## Tests

- `KeycloakJwtAuthoritiesConverterTest` (4 Tests):
  - Realm-Rollen werden zu `X` + `ROLE_X`,
  - Resource-Client-Rollen ebenso,
  - ohne Claims keine Rollen,
  - Case-insensitive auf UPPERCASE.

Voller `./mvnw -T 1C test` -> **BUILD SUCCESS**, 475 Tests.

Aenderungen an bestehenden Tests: **keine**. Die WebMvcTest-
Slices excluden weiterhin `SecurityAutoConfiguration`, sind
also blind gegenueber den neuen `@PreAuthorize` - das ist
bewusst so, weil ein echter Security-Test einen
Integrations-Stack erfordert.

## NICHT in dieser Iteration

- **Security-Integrationstest** mit echtem JWT-Flow (wuerde
  Keycloak oder einen Fake-Authorization-Server brauchen).
  Steht als Testcontainers-Folgearbeit in `offene-punkte.md`.
- **UI-Menue-Filterung nach Rollen** - landet in
  Iteration 24 (UI-Ueberarbeitung + Theming).
- **Tenant-Scope der Rollen** (z.&nbsp;B.
  `CVM_ADMIN@bkk-nord`) - aktuelles Modell ist flach.
