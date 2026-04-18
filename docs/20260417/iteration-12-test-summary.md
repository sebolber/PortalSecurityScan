# Iteration 12 - RAG-Pipeline - Test-Summary

**Jira**: CVM-31
**Datum**: 2026-04-18

## Testlauf

```
./mvnw -T 1C test  BUILD SUCCESS  (~70 s)
```

## Neue Tests

### `cvm-llm-gateway` (+7)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `FakeEmbeddingClientTest` | 5 | 1536 Dim, deterministisch, unterschiedlich, normalisiert, null-Eingabe. |
| `OllamaEmbeddingClientTest` | 2 | Happy-Path mit Pad, fehlendes Feld wirft. |

### `cvm-ai-services` (+16)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `ChunkerTest` | 4 | Leer, kurz, &gt;1500 Zeichen, deterministisch. |
| `IndexingServiceTest` | 6 | Assessment, Dedup, Advisory, unbekannt, leer, indexAll. |
| `RetrievalServiceTest` | 4 | Sortierung, Modell-Filter, topK&lt;=0, vector-Literal. |
| `AssessmentApprovedRagListenerTest` | 2 | Trigger, Fehler-schluck. |

### `cvm-api` (+1)
| Testklasse | Anzahl | Kurz |
|---|---:|---|
| `RagAdminControllerWebTest` | 1 | POST /reindex liefert chunks. |

## Gesamt-Testlage

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

## Architektur

- `ModulgrenzenTest` + `SpringBeanKonstruktorTest` -&gt; gruen.
- `cvm-llm-gateway` haengt weiter NUR von `cvm-domain` ab.
- `cvm-ai-services` ist die einzige Stelle, an der RAG-Logik die
  Persistenz und das Gateway zusammenfuehrt.

## Coverage

- Kernlogik (`Chunker`, `IndexingService`, `RetrievalService`,
  `FakeEmbeddingClient`, `Listener`) durch zielgenaue Unit-Tests
  abgedeckt.
- Native pgvector-Query (`findSimilar`) wird durch Repository-Mocks
  abgedeckt; Postgres-Integrationstest bleibt Docker-skipped.
