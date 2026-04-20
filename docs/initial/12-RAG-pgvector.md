# Iteration 12 – RAG-Pipeline (pgvector + Embeddings)

**Jira**: CVM-31
**Abhängigkeit**: 11
**Ziel**: Historische Assessments, Advisory-Texte und Upstream-Kommentare als
Retrieval-Quelle bereitstellen, damit KI-Vorschläge auf eigener Erfahrung fußen.

---

## Kontext
Konzept v0.2 Abschnitt 3 (Architektur) und Abschnitt 6.1 (Cascade Stufe 3).
pgvector wurde in Iteration 00 aktiviert. Embeddings laufen über denselben
Gateway wie LLM-Calls (Audit).

## Scope IN
1. `AiEmbedding`-Entity + Flyway `V0011__rag.sql`:
   - `ai_embedding`: `id`, `document_type` (`ASSESSMENT|ADVISORY|COMMIT|PROFILE_SNAPSHOT|DOC`),
     `document_ref` (freier Schlüssel, z. B. `assessment:<uuid>`),
     `chunk_text`, `embedding VECTOR(1536)`, `model_id`, `created_at`.
   - Index: `CREATE INDEX ... USING ivfflat (embedding vector_cosine_ops)`.
2. `EmbeddingClient` als Teil des LLM-Gateways (neuer `UseCase="EMBEDDING"`,
   Audit identisch).
3. `IndexingService`:
   - `indexAssessment(assessmentId)` bei `AssessmentApprovedEvent`.
   - `indexAdvisory(cveId)` bei CVE-Anreicherung (Iteration 03).
   - `indexAll()` für initialen Aufbau / Re-Index.
   - Chunking: max 1500 Zeichen pro Chunk, Überlappung 150.
4. `RetrievalService.similar(type, queryText, topK, filter)` – liefert
   Chunks mit Score.
5. Konfigurierbare Embedding-Modelle (Claude/Anthropic via API,
   lokales Fallback über Ollama mit `nomic-embed-text`).
6. Re-Index-Admin-Endpunkt `POST /api/v1/admin/rag/reindex`.
7. Initial-Seed aus historischen Wiki-Bewertungen (optional, als
   separater Import-Job): CSV-/Markdown-Import → synthetische
   `Assessment`-Datensätze mit Flag `migrated=true`, dann indexieren.

## Scope NICHT IN
- Nutzung im Cascade (Iteration 13).
- Copilot (Iteration 14).

## Aufgaben
1. pgvector-Integration via Hibernate-Type (Custom-`UserType` oder
   JDBC-direkter Zugriff für Vector-Spalten; Empfehlung: Native-Query für
   Similarity-Search, JPA nur für CRUD).
2. Dimensionalität aus Modell ableiten (Claude/Anthropic: 1536; Ollama
   variabel). Persistierte Embeddings tragen `model_id`, Mischung
   verschiedener Modelle in einer Suche wird abgewiesen.
3. Locale-aware: Deutsch/Englisch werden gemischt. Keine
   Sprache-spezifische Vorbehandlung ausser Normalisierung.
4. Metriken: Indexierungs-Durchsatz, Latenz, Embed-Kosten, RAG-Treffer-
   Score-Verteilung.

## Test-Schwerpunkte
- `EmbeddingClientTest`: WireMock für Claude-Embeddings.
- `IndexingServiceTest`: Chunking deterministisch, Duplicate Detection.
- `RetrievalServiceTest`: gegen Testcontainers-Postgres mit pgvector,
  Similarity-Rangfolge.
- Integrationstest: `AssessmentApprovedEvent` → Embedding persistiert
  → Retrieval findet es.
- `@DisplayName`: `@DisplayName("RAG: Assessment wird nach Approve eingebettet und ist ueber Similarity-Search auffindbar")`

## Definition of Done
- [ ] Embeddings produzierbar, speicherbar, durchsuchbar.
- [ ] Re-Index-Endpunkt funktioniert.
- [ ] Kostentracking sichtbar.
- [ ] Coverage `cvm-application/rag` ≥ 85 %.
- [ ] Fortschrittsbericht.
- [ ] Commit: `feat(rag): pgvector-gestuetzte Retrieval-Pipeline fuer historische Kontexte\n\nCVM-31`

## TDD-Hinweis
pgvector-Queries sollten gegen echte (Testcontainers-)Postgres getestet
werden, nicht Mock. Embeddings in Tests: deterministische Fake-Embeddings
(Hash → fester Vektor) über einen `FakeEmbeddingClient`, der in
CI-Profile aktiv ist.

## Abschlussbericht
Standard.
