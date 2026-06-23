# Galactic.Shipwrights

**Shipwrights, Manners & Swains**

The Better ideas of Trade and the Better Unions of Trades

---

## Overview

Galactic Shipwrights is an automated research, analysis, and speculation platform that crawls open-access internet sources—books, academic papers, images, technical manuals, government documents—and builds an AI-driven knowledge model from the collected corpus.

The system discovers patterns across sources, generates hypotheses about topic relevance and cross-domain correlations, and relays its findings to collaborative research servers.

## Architecture

```
┌─────────────┐     ┌────────────────────┐     ┌──────────────────┐
│   Reacher   │────▶│  src/edifiction/    │────▶│ SpeculatorTrainer│
│ (45 sources)│     │  (retrieved data)   │     │ (TF-IDF model)   │
└─────────────┘     └────────────────────┘     └────────┬─────────┘
                             │                          │
                             ▼                          ▼
                    ┌────────────────┐         ┌──────────────┐
                    │ Dictionary     │         │  Speculator   │
                    │ Profiler       │         │  (analysis)   │
                    └───────┬────────┘         └──────┬───────┘
                            │                         │
                            ▼                         ▼
                   dictionary.list.txt        ┌──────────────┐
                                              │  MySQL DB    │
                                              │ (ShipWrights)│
                                              └──────┬───────┘
                                                     │
                                                     ▼
                                          Green.Durham.Grass.and.Herb
                                              (port 20000)
```

## Components

### Reacher (`src/utils/Reacher.java`)
Web intelligence crawler. Connects to 45 configured sources, probes their capabilities (APIs, feeds, robots.txt, sitemaps), and fetches real content using source-specific strategies. Uses Gson for JSON, Jsoup for HTML/XML. Saves results to `src/edifiction/SOURCE/`.

### DictionaryProfiler (`src/utils/DictionaryProfiler.java`)
Scans all retrieved data for words, builds a master vocabulary, and looks up definitions via the free Dictionary API. Outputs an alphabetically sorted `dictionary.list.txt`.

### SpeculatorTrainer (`src/utils/SpeculatorTrainer.java`)
Builds the AI knowledge model from the corpus:
1. Term frequency vectors per source
2. TF-IDF weighting for topic significance
3. Cross-source correlation discovery
4. Persists weights to `trainer/speculator.model` and MySQL

### Speculator (`src/utils/Speculator.java`)
Analyzes the edifiction corpus using the trained model. Generates scored speculations with future projections. Sends high-interest findings to known servers on port 20000.

### SpeculatorDB (`src/utils/SpeculatorDB.java`)
MySQL persistence layer for the `ShipWrights` database. Stores speculations, posits, hypotheses, and correlations. **Requires a valid Installer ID from Max Rupplin - MEARVK LLC** (format: `MEARVK-XXXX-XXXX-XXXX`).

### Source Modules (`src/modules/`)
Per-source connection handlers extending `SourceModule`. Each knows its site's API, search protocol, and content format. Base class handles HTTP/HTTPS (port 80/443) awareness and SSL certificate exchange.

### Etymology Definitions (`src/*/definitions.xml`)
Each domain term (architect, bove, builder, nail, person, rudder, society, swain, town) has a `definitions.xml` tracing it back to Gaelic (1492 and earlier), Germanic, Italian, Latin, and PIE roots.

## Configuration

| File | Purpose |
|------|---------|
| `src/config.xml` | Reacher & DictionaryProfiler settings |
| `src/sources.xml` | 45 internet sources to crawl |
| `src/database.xml` | MySQL connection (requires Installer ID) |
| `src/known.20000.xml` | Green.Durham.Grass.and.Herb server registry |

## Requirements

- Java 17+
- MySQL 8.x (for persistence features)
- Valid Installer ID from Max Rupplin - MEARVK LLC
- Internet connectivity for source crawling

## Build & Run

```bash
# Compile
javac -cp "jars/*" -sourcepath src -d out src/Main.java

# Run
java -cp "out:jars/*" Main
```

## Execution Order

1. **Reacher** — Crawls all 45 sources, saves to `src/edifiction/`
2. **DictionaryProfiler** — Catalogs words, looks up definitions
3. **SpeculatorTrainer** — Trains model (skipped if `trainer/speculator.model` exists)
4. **Speculator** — Analyzes corpus, generates hypotheses, relays to servers

## Dependencies (jars/)

| JAR | Version | Purpose |
|-----|---------|---------|
| gson | 2.11.0 | JSON parsing |
| jsoup | 1.18.1 | HTML/XML parsing |
| rome | 2.1.0 | RSS/Atom feeds |
| jdom2 | 2.0.6.1 | XML DOM |
| slf4j | 2.0.13 | Logging |
| DJL API | 0.28.0 | AI inference |
| MySQL Connector/J | 8.4.0 | Database driver |

## Data Formats

- `.data` — Raw retrieved content from sources
- `.rdns` — Research Distillation & Notation System (paragraph-form academic prose)
- `.model` — Trained AI model (TF-IDF weights, profiles, correlations)
- `definitions.xml` — Multilingual etymology per domain term

## License & Ownership

**Max Rupplin - MEARVK LLC**

$52,000,000,000 - 00

---

*That Many men Search for an the Idea*
*That Many Cities services Wants*
*That Many Cities service Mens Wants*
*That Many Waters are Streams*
*That the US is Strong and is the USA*
*That Better waters exist and that better Streams are waters*
*That Better women Result and better Results serve Women and Water*
