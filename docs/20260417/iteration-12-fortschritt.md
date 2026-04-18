# Iteration 12 - RAG-Pipeline (pgvector + Embeddings) - Fortschritt

**Jira**: CVM-31
**Datum**: 2026-04-18
**Branch**: `claude/iteration-10-pdf-report-C9sA4`

## Zusammenfassung

Embeddings sind erzeugbar, persistierbar und durchsuchbar. Der
Standard-Embedding-Provider in CI/Default-Profil ist
`FakeEmbeddingClient` (deterministisch, hash-basiert). Real-Adapter
gegen Ollama (`/api/embeddings`) ist eingebaut und ueber
`cvm.llm.embedding.fake=false` aktivierbar. Die RAG-Logik lebt in
`cvm-ai-services/rag` (CLAUDE.md-Modulgrenzen sauber gehalten:
`application` darf den Gateway nicht direkt ansprechen). Persistenz
ueber neue `ai_embedding`-Tabelle mit pgvector + ivfflat-Index.
`AssessmentApprovedEvent` triggert automatisch die Indexierung.
Admin-Endpoint `POST /api/v1/admin/rag/reindex` ist aktiv.

## Umgesetzt

### Persistenz (`cvm-persistence/ai`)
- Entity `AiEmbedding` mit `vector(1536)`-Spalte (PGvector-Typ).
- Repository inkl. nativer Cosine-Distance-Query
  (`embedding <=> CAST(:queryVector AS vector)`),
  Pruefung auf `model_id` und `document_type`.
- Flyway `V0014__rag.sql`: Tabelle, ivfflat-Index, Unique
  (`document_type, document_ref, chunk_index, model_id`).
- `cvm-persistence/pom.xml` hat `org.postgresql:postgresql`-Scope von
  `runtime` auf `compile` umgestellt (PGvector erbt von PGobject).

### LLM-Gateway (`cvm-llm-gateway/embedding`)
- Interface `EmbeddingClient` + `EmbeddingResponse`-Record.
- `FakeEmbeddingClient`: SHA-256-basiert, deterministisch, L2-norm,
  1536 Dimensionen. Default-Bean (greift, solange kein anderer
  Embedding-Adapter aktiv ist).
- `OllamaEmbeddingClient`: RestClient gegen `/api/embeddings`,
  Pad/Truncate auf 1536 Dim, WireMock-Test. Aktiv ueber
  `cvm.llm.embedding.fake=false`.

### AI-Services / RAG (`cvm-ai-services/rag`)
- `Chunker`: 1500 Zeichen pro Chunk, 150 Ueberlappung,
  whitespace-bewusst, deterministisch.
- `IndexingService`:
  - `indexAssessment(uuid)` - liest Assessment, baut Text aus CVE,
    Severity, Source, Rationale, Quellfeldern.
  - `indexAdvisory(uuid)` - baut Text aus Cve-Feldern.
  - `indexAll()` - voller Re-Index.
  - Deduplikation: `deleteByDocumentTypeAndDocumentRef` vor jeder
    Indexierung -&gt; idempotent.
- `RetrievalService.similar(type, queryText, topK)` mit Filter auf
  `model_id` (Invariante: kein Mischen von Modellen).
- `AssessmentApprovedRagListener` (`@EventListener` auf
  `AssessmentApprovedEvent`). Schluckt Indexing-Fehler bewusst, damit
  der Approve-Flow nicht blockiert wird.

### REST (`cvm-api/rag`)
- `RagAdminController` mit `POST /api/v1/admin/rag/reindex`
  (CVM_ADMIN). Liefert JSON `{ "chunks": <int> }`.

### Konfiguration
- Property `cvm.llm.embedding.fake=true` (Default) liefert den
  FakeEmbeddingClient. `cvm.llm.embedding.ollama.base-url` /
  `.model` konfigurieren den realen Adapter.

## Pragmatische Entscheidungen

- **Modul-Verlagerung von `application/rag` nach `ai-services/rag`**:
  Der Prompt nennt `cvm-application/rag`. CLAUDE.md verbietet
  `application -&gt; llm-gateway`. Wir bleiben strikt: RAG lebt im
  `cvm-ai-services`-Modul, das per Architekturregel beide kennen darf.
- **FakeEmbeddingClient als Default-Bean**: Tests, lokale
  Entwicklung und CI laufen ohne externe Abhaengigkeit. Realer
  Embedding-Adapter laesst sich per Property einschalten.
- **Kein Anthropic-Embedding-Adapter**: Die offizielle Anthropic-
  Embedding-API ist 2026 noch in Beta. Wir liefern den Ollama-
  Adapter (`nomic-embed-text`) als reales Modell und das
  Fake-Modell fuer Tests; sobald Anthropic die Embedding-API
  stabilisiert, kann ein zweiter Adapter ergaenzt werden.
- **Embedding-Audit ueber den `AiCallAuditService`**: Aktuell wird
  der Embedding-Pfad noch nicht durch den Audit-Wrapper geleitet.
  Der Hook ist vorbereitet (use case `"EMBEDDING"` ist als String
  reserviert). Zur Vereinfachung der Iteration laesst der
  IndexingService die Embedding-Calls direkt durch; die
  Audit-Integration folgt in Iteration 13, sobald der Embedding-
  Pfad real angeworfen wird.
- **Pad/Truncate auf 1536**: Ollama `nomic-embed-text` liefert 768
  Dimensionen. Statt fuer jedes Modell eine eigene DB-Spalte zu
  haben, padden wir auf 1536 (Anthropic-Default-Dimension). Cosine-
  Aehnlichkeit bleibt gueltig, weil Padding-Werte 0 sind.
- **postgresql-Dependency-Scope**: `runtime` -&gt; `compile`, weil
  `com.pgvector.PGvector` von `org.postgresql.util.PGobject` erbt
  und im Compile-Pfad benoetigt wird.

## Tests (neu)

### cvm-llm-gateway (+7 = jetzt 52)
- `FakeEmbeddingClientTest` (5).
- `OllamaEmbeddingClientTest` (2).

### cvm-ai-services (+16 = jetzt 20)
- `ChunkerTest` (4).
- `IndexingServiceTest` (6).
- `RetrievalServiceTest` (4).
- `AssessmentApprovedRagListenerTest` (2).

### cvm-api (+1 = jetzt 30)
- `RagAdminControllerWebTest` (1).

### Gesamt-Testlauf
```
./mvnw -T 1C test  BUILD SUCCESS  (~70 s)
```

| Modul | Gruen | Skipped | Rot |
|---|---:|---:|---:|
| cvm-domain | 4 | 0 | 0 |
| cvm-persistence | 0 | 6 | 0 |
| cvm-application | 122 | 0 | 0 |
| cvm-integration | 8 | 0 | 0 |
| cvm-llm-gateway | **52** | 0 | 0 |
| cvm-ai-services | **20** | 0 | 0 |
| cvm-api | **30** | 0 | 0 |
| cvm-app | 0 | 5 | 0 |
| cvm-architecture-tests | 8 | 0 | 0 |
| **Gesamt** | **244** | 11 | 0 |

Iteration 12 bringt **24 neue Tests** ins System.

## Nicht im Scope

- Echter Anthropic-Embedding-Adapter (folgt sobald die API
  stabilisiert ist).
- Cascade-/KI-Use-Cases (Iteration 13).
- Copilot (Iteration 14).
- Initial-Seed aus historischen Wiki-Bewertungen (separater
  Import-Job spaeter).
- Performance-Tuning des ivfflat-Index (Iteration 21).

## Offene Punkte (fuer naechste Iterationen)

- **Embedding-Audit**: `IndexingService` ruft den `EmbeddingClient`
  direkt. Sobald RAG produktiv wird, sollten Embedding-Calls
  ebenfalls durch den `AiCallAuditService` laufen
  (use case `EMBEDDING`).
- **`indexAll()` mit Pageable**: Aktuell `findAll()` -&gt; OOM bei
  grossen Datenmengen. Streaming/Pageable-Variante einbauen.
- **pgvector-Integrationstest** (Testcontainers) bleibt
  Docker-skipped.
- **Anthropic-Embedding-Adapter**, sobald `text-embedding-3-large`
  / `text-embedding-3-small` stabil sind.
- **Locale-aware Vorbehandlung**: aktuell rohe Strings -&gt; Modell.
  Falls deutsch/englisch-spezifische Normalisierung relevant wird
  (z.B. Umlaute), separater Pre-Processor.

## Ausblick Iteration 13
KI-Vorbewertung (Cascade Stufe 3): Wenn Regel + REUSE keinen
Vorschlag liefern, fragt der Cascade-Service einen
KI-Vorbewerter. Der nutzt den `RetrievalService` aus dieser
Iteration als RAG-Quelle und das LLM-Gateway aus Iteration 11.

## Dateien (wesentlich, neu)

### Persistenz
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiEmbedding.java`
- `cvm-persistence/src/main/java/com/ahs/cvm/persistence/ai/AiEmbeddingRepository.java`
- `cvm-persistence/src/main/resources/db/migration/V0014__rag.sql`
- `cvm-persistence/pom.xml` (postgresql Scope)

### LLM-Gateway
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/embedding/EmbeddingClient.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/embedding/FakeEmbeddingClient.java`
- `cvm-llm-gateway/src/main/java/com/ahs/cvm/llm/embedding/OllamaEmbeddingClient.java`
- `cvm-llm-gateway/src/test/java/com/ahs/cvm/llm/embedding/FakeEmbeddingClientTest.java`
- `cvm-llm-gateway/src/test/java/com/ahs/cvm/llm/embedding/OllamaEmbeddingClientTest.java`

### AI-Services
- `cvm-ai-services/src/main/java/com/ahs/cvm/ai/rag/Chunker.java`
- `cvm-ai-services/src/main/java/com/ahs/cvm/ai/rag/IndexingService.java`
- `cvm-ai-services/src/main/java/com/ahs/cvm/ai/rag/RetrievalService.java`
- `cvm-ai-services/src/main/java/com/ahs/cvm/ai/rag/AssessmentApprovedRagListener.java`
- `cvm-ai-services/src/test/java/com/ahs/cvm/ai/rag/ChunkerTest.java`
- `cvm-ai-services/src/test/java/com/ahs/cvm/ai/rag/IndexingServiceTest.java`
- `cvm-ai-services/src/test/java/com/ahs/cvm/ai/rag/RetrievalServiceTest.java`
- `cvm-ai-services/src/test/java/com/ahs/cvm/ai/rag/AssessmentApprovedRagListenerTest.java`

### API
- `cvm-api/src/main/java/com/ahs/cvm/api/rag/RagAdminController.java`
- `cvm-api/src/test/java/com/ahs/cvm/api/rag/RagAdminControllerWebTest.java`
- `cvm-api/src/test/java/com/ahs/cvm/api/rag/RagAdminTestApi.java`

### Docs
- `docs/20260417/iteration-12-plan.md`
- `docs/20260417/iteration-12-fortschritt.md`
- `docs/20260417/iteration-12-test-summary.md`
