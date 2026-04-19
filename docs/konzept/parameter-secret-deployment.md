# Deployment-Doku: `cvm.encryption.parameter-secret`

**Jira**: CVM-306
**Stand**: 2026-04-19 (Iteration 69)

## Worum geht es

Der System-Parameter-Store verschluesselt sensible Eintraege
(`sensitive=true` im Katalog, aktuell
`cvm.llm.claude.api-key`, `cvm.feed.nvd.api-key`,
`cvm.feed.ghsa.api-key`, `cvm.ai.fix-verification.github.token`)
per **AES-GCM**. Der Master-Key wird aus der Konfigurations-
Property `cvm.encryption.parameter-secret` per SHA-256 abgeleitet
(siehe `com.ahs.cvm.application.parameter.SystemParameterSecretCipher`).

Der Dev-Default
```
cvm-dev-default-parameter-secret
```
(Konstante in `SystemParameterSecretCipher` aus
Iteration 45) ist ausschliesslich fuer lokale `scripts/start.sh`-
Laeufe gedacht. **In Staging und Produktion muss die Property
ueber eine geheime Quelle gesetzt werden.**

## Warum nicht im Repo

- Offline/Backup-gefaehrdet: Ein Leak des Master-Keys macht die
  gesamte AES-GCM-Verschluesselung wertlos - das Angreifermodell
  sieht den Master-Key explizit nicht vor.
- Mandantenfaehigkeit: Ein Master-Key gilt global fuer alle
  Tenants. Rotation (siehe unten) muss kontrolliert erfolgen.
- Compliance (BSI C5, DSGVO): Secrets gehoeren in Vault/
  OpenShift-Secret, nicht in Git-Historie.

## Quellen pro Umgebung

| Umgebung    | Bezugsquelle                                        |
| ----------- | --------------------------------------------------- |
| Dev-lokal   | Dev-Default aus `SystemParameterSecretCipher`       |
| CI (Karma/IT) | GitLab CI Variable `CVM_PARAMETER_SECRET` (masked) |
| REF-TEST    | OpenShift-Secret `cvm-parameter-secret` im Namespace `cvm-ref-test` |
| ABN-TEST    | OpenShift-Secret `cvm-parameter-secret` im Namespace `cvm-abn-test` |
| PROD        | Vault-Secret `secret/cvm/prod/parameter-secret`, via OpenShift-Secret-Store-CSI gemountet |

Der Wert MUSS mindestens 24 Zeichen lang sein, zufaellig (Base32
oder Base64), pro Umgebung unterschiedlich. Verwende z.B.
`openssl rand -base64 32` zum Erzeugen.

## OpenShift-Secret-Template

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: cvm-parameter-secret
  namespace: cvm-prod
type: Opaque
stringData:
  # CVM_ENCRYPTION_PARAMETER_SECRET wird in der Deployment-Spec
  # unter env.valueFrom.secretKeyRef referenziert.
  CVM_ENCRYPTION_PARAMETER_SECRET: "<aus Vault mounten, nicht inline>"
```

Der Key-Name `CVM_ENCRYPTION_PARAMETER_SECRET` spiegelt das
Property `cvm.encryption.parameter-secret` (Spring-Relaxed-
Binding). In `application.yaml` wird bewusst **kein Default**
gesetzt; falls doch, faellt er auf den Dev-Default zurueck und
verhindert keinen Prod-Fehlstart.

## Rollout-Checkliste (erste Aktivierung)

1. Generieren: `openssl rand -base64 32` -> in Vault oder
   OpenShift-Secret ablegen (siehe Template).
2. `Deployment`/`DeploymentConfig` des `cvm-app`-Pods referenziert
   das Secret per `envFrom.secretRef` oder `env.valueFrom`.
3. Pod neu starten. Im Log erscheint:
   `SystemParameterSecretCipher Bean konstruiert`. Schlaegt die
   Entschluesselung eines bestehenden Secrets fehl, kommt
   `Parameter-Entschluesselung fehlgeschlagen` als
   `IllegalStateException`.
4. Smoke-Test: Admin legt einen Dummy-Eintrag
   `cvm.feed.nvd.api-key` ab und liest ihn wieder. Das Backend
   zeigt im Admin-UI nur `(verschluesselt)`.
5. Alarm: ohne erfolgreichen Smoke-Test nicht auf Prod deployen.

## Key-Rotation

Ziel: aktiver Master-Key wechselt jaehrlich oder nach einem
Verdacht-Event (Leak, Admin-Abgang).

**Vorgehen "Dual-Write":**

1. Neuen Master-Key in Vault hinterlegen (z.B.
   `secret/cvm/prod/parameter-secret.next`).
2. Admin-Job (manuell, SQL + REST):
   - Pro mandantem sensitiven Parameter den Klartext ueber den
     aktuellen Resolver lesen.
   - Wert loeschen und erneut anlegen - der
     `SystemParameterService` verschluesselt bei jedem `save(...)`
     mit dem aktuell aktiven Master-Key.
3. Sobald alle Eintraege mit dem neuen Key gespeichert sind, den
   `cvm.encryption.parameter-secret` in der Deployment-Konfig auf
   den neuen Wert zeigen lassen. Rolling Restart.
4. Alten Key in Vault zur Sicherheit 30 Tage vorhalten, danach
   vernichten.

Alternative (groessere Datenmengen): eigenes Rotation-CLI
(`cvm-parameter-rotate`) in Iteration 70+. Aktuell wird das
manuelle Vorgehen als "kleine Liste" akzeptiert, weil die
Tabelle nur Konfigurationswerte (< 100 Eintraege pro Tenant)
haelt.

## Backup-Strategie

- Das OpenShift-/Vault-Secret selbst liegt bei der zentralen
  Infrastruktur-Gruppe und wird nach ihrem Standardplan
  gesichert (taegliches Snapshot, 30 Tage Retention).
- Die PostgreSQL-Datenbank wird unveraendert per WAL-Archiv +
  pgbackrest gesichert. Die verschluesselten Eintraege sind ohne
  Master-Key nutzlos, daher nicht separat verschluesselt.
- **Wichtig**: Backups der DB und des Master-Keys gehoeren in
  getrennte Tresore/Accounts. Kein Cross-Restore ohne beide
  Artefakte.

## Abgrenzung zu anderen Secrets

- `cvm.encryption.sbom-secret`: AES-GCM-Masterkey fuer SBOM-
  Verschluesselung. Lebt weiterhin in `application.yaml` bzw.
  Vault (Henne-Ei: Parameter-Store haengt davon NICHT ab).
- `cvm.security.jwt.public-key`: Keycloak-OIDC-Key, aus
  `spring.security.oauth2.resourceserver.jwt.*`. Nicht im
  Parameter-Store (steht auf Non-Migrate-Liste).
- `spring.datasource.password`: DB-Credentials, aus
  OpenShift-Secret via `spring.*`. Nicht im Parameter-Store.

## Fehlerbehandlung

- **Start ohne Secret**: Spring akzeptiert den Dev-Default, der
  Pod bootet. Bestehende verschluesselte Eintraege lassen sich
  nicht entschluesseln -> `IllegalStateException` beim ersten
  `resolve(...)` des betroffenen Keys. In PROD sollte das ein
  Readiness-Probe-Ereignis ausloesen; der Pod wird vom Load-
  Balancer genommen.
- **Falscher Secret-Wert**: gleiches Symptom wie oben. Rollback
  auf den vorherigen Secret-Wert loest das Problem.
- **Key-Rotation unvollstaendig**: Ein Teil der Eintraege liegt
  noch mit dem alten, ein Teil mit dem neuen Key vor. Der
  Cipher kennt nur einen Key zur Zeit. Mitigation: Rotation
  immer "Schreib-Phase"-weise abschliessen (siehe oben) und
  bei Abbruch den Rotation-Job von vorne starten.

## Referenzen

- `docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md` §9
  (Sicherheits-Leitplanken).
- `docs/20260419/offene-punkte.md` Abschnitt
  "Konfigurationsverwaltung".
- Iteration-Reports:
  - Iteration 45 (Einfuehrung des Ciphers, Secret-Seeds).
  - Iteration 46 (ArchUnit + E2E-Test).
  - Iteration 66/67/68 (Callsite-Migration der Adapter auf den
    Resolver).
