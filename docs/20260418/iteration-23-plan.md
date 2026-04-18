# Iteration 23 - Rollen-Verdrahtung Keycloak/Spring - Plan

**Jira**: CVM-54
**Branch**: `claude/iteration-22-continuation-GSK7w`
**Datum**: 2026-04-18

## Auftrag

Die im Konzept und in `offene-punkte.md` genannten feingranularen
Rollen waren zwar fachlich gemeint, aber weder im Keycloak-Realm
noch im Spring-Security-Stack verdrahtet. Nur `CVM_ADMIN` hat an
8 Stellen via `@PreAuthorize` gezogen - und selbst dort ohne
JWT-Claim-Extraktion, sprich: unter realer Keycloak-Konfiguration
haette die Annotation gar nicht gegriffen.

## Scope

1. **Keycloak-Realm** (`infra/keycloak/dev-realm.json`):
   - neue Rollen `CVM_REVIEWER`, `CVM_PROFILE_AUTHOR`,
     `CVM_PROFILE_APPROVER`, `CVM_RULE_AUTHOR`, `CVM_RULE_APPROVER`,
     `CVM_REPORTER`, `AI_AUDITOR`,
   - Default-Rollen fuer die drei Dev-User
     (`t.tester`, `a.admin`, `j.meyer`).

2. **JWT-Rollen-Extraktion** (`cvm-api/config`):
   - Neuer `KeycloakJwtAuthoritiesConverter` liest
     `realm_access.roles` + `resource_access.*.roles` und
     legt je Rolle sowohl die nackte Authority (`CVM_ADMIN`)
     als auch die `ROLE_`-Variante (`ROLE_CVM_ADMIN`) ab.
   - In `WebSecurityConfig` wird der Converter ueber einen
     `JwtAuthenticationConverter`-Bean aktiviert.

3. **Controller-Annotationen**:
   - `ProfileController`: PUT -> `CVM_PROFILE_AUTHOR|CVM_ADMIN`,
     POST `/approve` -> `CVM_PROFILE_APPROVER|CVM_ADMIN`.
   - `RulesController`: POST -> `CVM_RULE_AUTHOR|CVM_ADMIN`,
     `/activate` -> `CVM_RULE_APPROVER|CVM_ADMIN`,
     `/dry-run` -> `CVM_RULE_AUTHOR|CVM_RULE_APPROVER|CVM_ADMIN`.
   - `AssessmentsController`: POST -> `CVM_ASSESSOR|CVM_ADMIN`,
     `/approve` -> `CVM_APPROVER|CVM_ADMIN`,
     `/reject` -> `CVM_APPROVER|CVM_REVIEWER|CVM_ADMIN`.
   - `ReportsController`: POST `/hardening` ->
     `CVM_REPORTER|CVM_ADMIN`, alle Lese-Endpunkte ->
     `CVM_VIEWER|CVM_REPORTER|CVM_ADMIN`.
   - `AiAuditController`: bleibt `AI_AUDITOR|CVM_ADMIN`
     (war schon so; wird durch Realm jetzt auch real nutzbar).

4. **Tests**:
   - `KeycloakJwtAuthoritiesConverterTest` (realm, resource,
     case-insensitive, leer).
   - Bestehende WebMvcTest-Slices laufen unveraendert - sie
     excluden `SecurityAutoConfiguration`.

## Nicht in dieser Iteration

- Rollen-basierte UI-Menue-Filterung im Angular-Frontend -
  Thema Iteration 24 (UI-Ueberarbeitung + Theming).
- RLS-Policies und Scope-Snapshots (weiter offen).
- Tenant-basierte Rolle-Scopes
  (z.&nbsp;B. `CVM_ADMIN@bkk-nord`) - bleibt bewusst flach.
