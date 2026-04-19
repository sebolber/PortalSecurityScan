# Iteration 69 - Fortschritt: Deployment-Doku parameter-secret

**Jira**: CVM-306
**Datum**: 2026-04-19

## Was wurde gebaut

Neue Doku-Datei
`docs/konzept/parameter-secret-deployment.md`. Inhalte:

- **Worum geht es**: Verweis auf `SystemParameterSecretCipher`
  aus Iteration 45, SHA-256-Ableitung aus
  `cvm.encryption.parameter-secret`.
- **Warum nicht im Repo**: Leak-Risiko, Mandantenfaehigkeit,
  BSI-C5/DSGVO-Hinweis.
- **Quellen pro Umgebung**: Dev (Dev-Default), CI
  (GitLab-CI-Variable `CVM_PARAMETER_SECRET`, masked), REF/ABN
  (Namespace-spezifische OpenShift-Secrets), PROD (Vault-Secret
  per CSI gemountet).
- **OpenShift-Secret-Template**: YAML-Snippet mit `stringData:
  CVM_ENCRYPTION_PARAMETER_SECRET`.
- **Rollout-Checkliste** (5 Schritte): Generieren, Secret-Mount,
  Restart, Smoke-Test, Alarm bei Fehlschlag.
- **Key-Rotation** ("Dual-Write", 4 Schritte): neuen Key
  hinterlegen, je Eintrag re-save, Deployment-Property
  umschalten, alten Key 30 Tage halten.
- **Backup-Strategie**: Secret + DB liegen in getrennten
  Tresoren; Restore verlangt beides.
- **Abgrenzung**: `cvm.encryption.sbom-secret`, Keycloak-JWT-
  Key, Spring-Datasource-Credentials stehen nicht im
  Parameter-Store (Non-Migrate).
- **Fehlerbehandlung**: Verhalten bei fehlendem Secret, falschem
  Secret und abgebrochener Rotation.

## Nicht-Aenderungen

- Kein Java-Code, kein Test, keine Flyway-Migration.

## Offen

Der offene Punkt in `docs/20260419/offene-punkte.md` unter
"cvm.encryption.parameter-secret ... Doku-Anforderung" ist
damit geschlossen.
