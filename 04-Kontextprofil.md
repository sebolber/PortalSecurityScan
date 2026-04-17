# Iteration 04 – Kontextprofil (YAML + Versionierung)

**Jira**: CVM-13
**Abhängigkeit**: 01
**Ziel**: Umgebungen bekommen versionierte, strukturierte Kontextprofile als
Grundlage für Regel-Engine und KI.

---

## Kontext
Konzept v0.2 Abschnitt 4.2 und 6.4. Profile sind YAML (siehe Beispiel im
Konzept), werden als JSONB persistiert, sind versioniert, Änderung triggert
`NEEDS_REVIEW` auf betroffenen Assessments.

## Scope IN
1. `ContextProfile`-Entity wurde in Iteration 01 angelegt; hier das
   fachliche Verhalten implementieren.
2. Profil-Schema definieren (JSON Schema aus YAML-Struktur) und versionieren
   (`schemaVersion: 1`).
3. `ContextProfileService`:
   - `latestFor(environmentId)` – aktuell gültige Version.
   - `proposeNewVersion(environmentId, payload)` – erzeugt `DRAFT`.
   - `approve(profileVersionId, secondApproverId)` – Vier-Augen-Prinzip,
     setzt neue Version aktiv, alte wird `SUPERSEDED`.
   - `diff(oldVersion, newVersion)` – liefert Feldliste mit Alt-/Neu-Wert.
4. Event `ContextProfileActivatedEvent` mit Diff-Liste.
5. Event-Listener `AssessmentReviewMarker`: setzt alle betroffenen
   Assessments auf `NEEDS_REVIEW`, wenn ein referenziertes Feld sich
   geändert hat. „Referenziert" = das Feld kommt im gespeicherten
   `rationaleSourceFields` des Assessments vor (neues Feld in
   `Assessment`, jsonb-Array von Profilpfaden, z. B.
   `["architecture.windows_hosts"]`).
6. REST-Endpunkte:
   - `GET /api/v1/environments/{id}/profile`
   - `PUT /api/v1/environments/{id}/profile` (liefert Draft-ID, kein sofortiger Switch)
   - `POST /api/v1/profiles/{versionId}/approve`
   - `GET /api/v1/profiles/{versionId}/diff?against=latest`
7. Flyway `V0007__profil_versionierung.sql` (falls Ergänzungen nötig:
   `profile_state`, `superseded_at`, `rationale_source_fields` auf `assessment`).

## Scope NICHT IN
- Dialog-Assistent (Iteration 18).
- Regel-Engine-Nutzung (Iteration 05).
- UI (Iteration 08).

## Aufgaben
1. YAML-Parser (SnakeYAML) mit strenger Validierung gegen JSON Schema.
2. `JsonSchemaValidator`-Komponente, Fehlermeldungen deutschsprachig und
   feldgenau.
3. `DiffBuilder` produziert deterministische, getypte Diffs.
4. Ereignisgetriebene Markierung der Assessments, Batch-Update mit
   `UPDATE assessment SET status='NEEDS_REVIEW' WHERE id IN (...)`.
5. Audit-Eintrag für jede Profiländerung.

## Test-Schwerpunkte
- `ContextProfileServiceTest`: Draft-Erzeugung, Approve, Vier-Augen
  (Autor ≠ Approver).
- `ProfileDiffTest`: tief verschachtelte Struktur, boolean-Flip,
  hinzugefügtes Feld, entferntes Feld.
- Integrationstest: Profil ändern → Event → betroffene Assessments auf
  `NEEDS_REVIEW`.
- Negativtest: Approver == Autor → `FourEyesViolationException`.
- `@DisplayName`: `@DisplayName("Profil: Aktivierung setzt betroffene Assessments auf NEEDS_REVIEW")`

## Definition of Done
- [ ] YAML-Profil speicherbar, versionierbar, freigebbar.
- [ ] Vier-Augen-Prinzip greift.
- [ ] Assessments werden auf NEEDS_REVIEW gesetzt.
- [ ] Coverage `cvm-application/profile` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(profile): Kontextprofil mit Versionierung und Diff-getriebenem NEEDS_REVIEW\n\nCVM-13`

## TDD-Hinweis
Der Diff-Builder ist Herzstück. Erst umfangreiche Diff-Testfälle, dann
Implementierung. **Ändere NICHT die Tests** bei Rot.

## Abschlussbericht
Standard.
