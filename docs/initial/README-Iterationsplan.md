# CVE-Relevance-Manager – Iterationsplan

*Sequentielle Roadmap für die Claude-Code-gestützte Umsetzung. Jede Iteration
entspricht einer Claude-Code-Session und baut auf der vorherigen auf.*

---

## Ausführungsmodus

Jede Iteration wird wie folgt gestartet:

```bash
cd /pfad/zum/cvm-repo
claude code
> Lies CLAUDE.md und docs/konzept/CVE-Relevance-Manager-Konzept-v0.2.md.
> Fuehre dann Iteration NN aus: docs/iterationen/NN-<titel>.md
```

Claude Code arbeitet den Iterations-Prompt strikt nach dem in `CLAUDE.md`,
Abschnitt 10 vorgegebenen Ablauf ab (Verstehen → Plan → Tests → Code →
Refaktor → Architekturprüfung → Abschlussbericht).

---

## Phase 0 – Initialisierung

| # | Titel | Dauer-Schätzung | Jira | Abhängigkeit |
|---|---|---|---|---|
| 00 | Repo-Bootstrap, `CLAUDE.md`, Maven-Multimodul, CI-Skeleton | ~1 Sitzung | CVM-1 | – |

---

## Phase 1 – MVP (deterministisch, ohne KI)

| # | Titel | Jira | Abhängigkeit |
|---|---|---|---|
| 01 | Domain-Kern (Produkt/Version/Umgebung/Scan/CVE) + Flyway | CVM-10 | 00 |
| 02 | SBOM-Ingestion (CycloneDX-Parser, POST `/scans`) | CVM-11 | 01 |
| 03 | CVE-Anreicherung (NVD/GHSA/KEV/EPSS) | CVM-12 | 01 |
| 04 | Kontextprofil (YAML, Versionierung, `NEEDS_REVIEW`-Trigger) | CVM-13 | 01 |
| 05 | Regel-Engine (deterministisch, Dry-Run) | CVM-14 | 04 |
| 06 | Bewertungs-Workflow (Queue, Approve, Vier-Augen) | CVM-15 | 02, 05 |
| 07 | Frontend-Shell (Angular 18, Keycloak, Layout) | CVM-16 | 00 |
| 08 | Bewertungs-Queue-UI (Queue, Detail, Approve/Override) | CVM-17 | 06, 07 |
| 09 | SMTP-Alerts (Trigger, Templates, Cooldown) | CVM-18 | 06 |
| 10 | Abschlussbericht-PDF (Thymeleaf + openhtmltopdf) | CVM-19 | 06 |

**Meilenstein M1**: Deterministisches MVP lauffähig, Trivy-Report verarbeitbar,
manuelle Bewertung möglich, Bericht generierbar.

---

## Phase 2 – KI-Grundlage

| # | Titel | Jira | Abhängigkeit |
|---|---|---|---|
| 11 | LLM-Gateway (Adapter, Prompts, `ai_call_audit`, Rate-Limit) | CVM-30 | M1 |
| 12 | RAG-Pipeline (pgvector, Embeddings, Similarity-Search) | CVM-31 | 11 |
| 13 | KI-Vorbewertung als Cascade-Stufe 3 | CVM-32 | 12 |
| 14 | Inline-Copilot + KI-Delta-Summary | CVM-33 | 13 |

**Meilenstein M2**: KI läuft parallel zur Regel-Engine; Vorschläge als
`PROPOSED` in Queue; Audit vollständig.

---

## Phase 3 – Tiefenautomatisierung

| # | Titel | Jira | Abhängigkeit |
|---|---|---|---|
| 15 | Reachability-Agent (Claude Code CLI gegen Git) | CVM-40 | 13 |
| 16 | Fix-Verifikation (Upstream-Release-Notes + Commits) | CVM-41 | 13 |
| 17 | KI-Regel-Extraktion (Nightly, Dry-Run-Auswertung) | CVM-42 | 13 |
| 18 | KI-Anomalie-Check + Profil-Assistent | CVM-43 | 13 |

**Meilenstein M3**: Automatisierungsquote im Dashboard sichtbar;
Regel-Extraktion liefert erste Vorschläge; Reachability läuft.

---

## Phase 4 – Governance & Skalierung

| # | Titel | Jira | Abhängigkeit |
|---|---|---|---|
| 19 | NL-Query-Dashboard + Executive-/Board-Report | CVM-50 | 14 |
| 20 | VEX-Export/Import + Waiver-Management | CVM-51 | 06 |
| 21 | Mandantenfähigkeit + CI/CD-Gate + Trends/KPIs | CVM-52 | alle |

**Meilenstein M4**: Produktivreifes System für Rollout auf mehrere Mandanten.

---

## Abhängigkeits-Graph (vereinfacht)

```
00
 ├── 01 ── 02 ── 03
 │    │    │     │
 │    ├── 04 ── 05 ── 06 ── 08 ── 10
 │    │                │         │
 │    │                └── 09    └── 19
 │    │                │
 │    │                └── 11 ── 12 ── 13 ── 14 ── 19
 │    │                                │
 │    │                                ├── 15
 │    │                                ├── 16
 │    │                                ├── 17
 │    │                                └── 18
 │    │
 │    └── 20 (parallel ab 06)
 │
 └── 07 ── 08
      │
      └── 21 (nach allen anderen)
```

---

## Arbeitsregeln über alle Iterationen

1. **Tests zuerst.** Jede Iteration startet mit fehlschlagenden Tests.
2. **Ändere NICHT die Tests**, wenn sie rot werden. Fixe den Produktionscode.
3. **ArchUnit-Tests** sind Pflicht. Modulgrenzen werden hart geprüft.
4. **Report-Disziplin**: jede Iteration hinterlässt einen Fortschrittsbericht
   unter `docs/YYYYMMDD/iteration-NN-fortschritt.md`.
5. **Conventional Commits**, Jira-Key im Footer.
6. **Keine Secrets** im Repository.
7. **Kein KI-Call ohne Audit** (ab Iteration 11).
8. **Vier-Augen-Prinzip** für Downgrades, Regel-Aktivierung,
   Profil-Freigabe, LLM-Anbieter-Wechsel.

---

*Stand: 17.04.2026*
