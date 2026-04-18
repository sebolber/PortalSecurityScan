# Iteration 12 - RAG-Pipeline (pgvector + Embeddings) - Plan

**Jira**: CVM-31
**Branch**: `claude/iteration-10-pdf-report-C9sA4` (Folgearbeit auf
demselben Branch wie Iteration 10/11).
**Abhaengigkeit**: Iteration 11 (LLM-Gateway + Audit).
**Ziel**: Historische Assessments und Advisory-Texte als Retrieval-
Quelle bereitstellen, damit KI-Vorschlaege auf eigener Erfahrung
fussen.

## Architektur-Grundsatz

Der Prompt nennt Coverage-Pfad `cvm-application/rag`. Das verstoesst
gegen die Modulgrenzen aus CLAUDE.md (`application -> domain,
persistence`; **nicht** `llm-gateway`). Die RAG-Logik braucht den
EmbeddingClient aus dem Gateway, also lebt sie in `cvm-ai-services/rag`,
das per CLAUDE.md `ai-services -> application, llm-gateway, integration`
das darf.

## Scope IN

1. **Persistenz** (cvm-persistence)
   - `AiEmbedding`-Entity mit `vector(1536)`-Spalte (Hibernate
     `@Column(columnDefinition = "vector(1536)")`, der PG-Treiber
     mappt String-Form `[0.1,0.2,...]`).
   - `AiEmbeddingRepository` (CRUD + Native-Query fuer
     `<=>` Cosine-Distance).
   - Flyway `V0014__rag.sql` (V0011 in 09 vergeben, V0014 ist naechste
     freie Nummer).
2. **LLM-Gateway**
   - Interface `EmbeddingClient.embed(text)` mit `EmbeddingResponse`.
   - `FakeEmbeddingClient` (deterministisch, hash-basiert) fuer Tests
     und CI-Profile (`cvm.llm.embedding.fake=true`).
3. **AI-Services / RAG** (cvm-ai-services/rag)
   - `Chunker.split(text)` deterministisch (max 1500 Zeichen, 150
     Ueberlappung).
   - `IndexingService.indexAssessment(...)`,
     `indexAdvisory(...)`, `indexAll()`.
   - `RetrievalService.similar(type, queryText, topK)`.
   - `AssessmentApprovedRagListener` reagiert auf
     `AssessmentApprovedEvent` und indiziert das Assessment.
4. **API**
   - `RagAdminController` mit `POST /api/v1/admin/rag/reindex`
     (CVM_ADMIN).

## Scope NICHT IN

- Echtes Anthropic-Embeddings-Adapter mit HTTP - das LLM-Gateway
  bekommt einen `FakeEmbeddingClient`-Default und einen
  `OllamaEmbeddingClient` als optionalen Real-Adapter (nur Skelett +
  WireMock-Test). Der vollstaendige Anthropic-Embedding-Endpoint
  bleibt fuer den ersten produktiven Einsatz offen, weil die API noch
  in Beta ist und sich aendert.
- Cascade-Integration (Iteration 13).
- Copilot (Iteration 14).
- Performance-Benchmark / Index-Tuning (folgt mit erstem Datenbestand).

## Tests

1. `ChunkerTest` - 1500/150-Logik, Determinismus, Leerstring,
   Sehr-langer-Text.
2. `FakeEmbeddingClientTest` - deterministisch (gleicher Text -&gt;
   gleicher Vektor).
3. `OllamaEmbeddingClientTest` - WireMock Happy-Path + Fehler.
4. `IndexingServiceTest` - Assessment-Indexierung mit Fake-Client,
   Duplicate Detection (gleicher document_ref ueberschreibt nicht
   stillschweigend, sondern bekommt neue chunks).
5. `RetrievalServiceTest` - Mock-Repository liefert sortierte Liste,
   Service mappt Score korrekt.
6. `AssessmentApprovedRagListenerTest` - Listener triggert Indexing.
7. `RagAdminControllerWebTest` - POST 202, Service wird angetriggert.

Keine echten pgvector-Tests in CI (Docker-skipped) - die Native-
Query bleibt durch Repository-Mocks abgedeckt; ein
`@Tag("docker")`-Slice-Test wird vorbereitet, aber nur ausgefuehrt
wenn Postgres da ist.

## Stopp-Kriterien

- Kein Embedding-Call ohne Audit (laeuft durch `AiCallAuditService`).
- Kein Mischen von Modellen in einer Suche
  (`RetrievalService` filtert auf `model_id`).
- Feature-Flag `cvm.llm.enabled=false` deaktiviert auch Embeddings;
  der `FakeEmbeddingClient` bleibt fuer Tests aktiv.
