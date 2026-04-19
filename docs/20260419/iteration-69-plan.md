# Iteration 69 - Plan: Deployment-Doku cvm.encryption.parameter-secret

**Jira**: CVM-306

## Ziel

Als Reaktion auf den offenen Punkt aus
`docs/20260419/offene-punkte.md` dokumentieren, wie
`cvm.encryption.parameter-secret` in produktiven Umgebungen
gesetzt wird, wie die Key-Rotation verlaeuft, und wie die Backup-
Strategie aussieht.

## Umfang

- Neue Datei `docs/konzept/parameter-secret-deployment.md` mit
  den Abschnitten:
  - Warum nicht im Repo
  - Quellen pro Umgebung (Dev, CI, REF/ABN/PROD)
  - OpenShift-Secret-Template
  - Rollout-Checkliste
  - Key-Rotation ("Dual-Write", 4 Schritte)
  - Backup-Strategie
  - Abgrenzung zu anderen Secrets (sbom, jwt, datasource)
  - Fehlerbehandlung

- Keine Code-Aenderung (reine Doku-Iteration).

## Abnahme

- Datei existiert, der offene Punkt in `offene-punkte.md` ist
  als erledigt markiert.
- `./mvnw -T 1C test` bleibt gruen (kein Run noetig, aber
  sicherheitshalber vermerkt).
