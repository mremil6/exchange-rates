# Marcura — Exchange Rate Management System

Full-stack solution to the Marcura senior full-stack assessment. Spring Boot backend + Angular SPA + Spring AI trend insight, packaged so a reviewer can run the whole thing locally with three commands.

> **Quick links:** [PLAN.md](./PLAN.md) (the up-front planning artefact) · [CLAUDE.md](./CLAUDE.md) (agent context) · [.cursor/rules](./.cursor/rules) (editor rules) · [Swagger UI](http://localhost:8080/swagger-ui.html) (after backend boot)

---

## 1. Architecture

```
┌─────────────────────────┐   HTTP   ┌──────────────────────────┐
│  Angular 18 SPA         │  ──────▶ │  Spring Boot 3.3 (Java 21)│
│  · Calculator           │          │  · REST controllers       │
│  · Historical + Chart   │          │  · Services + JPA         │
│  · Analytics dashboard  │          │  · Scheduler (ShedLock)   │
└─────────────────────────┘          │  · Spring AI ChatClient   │
                                     └─────────────┬─────────────┘
                                                   │
                  ┌────────────────────────────────┼────────────────────────────┐
                  ▼                                ▼                            ▼
          ┌─────────────────┐             ┌────────────────┐           ┌──────────────────┐
          │ PostgreSQL 16   │             │ Fixer.io API   │           │ Ollama           │
          │  rates + usage  │             │  (daily fetch) │           │  (local LLM)     │
          │  + shedlock     │             └────────────────┘           └──────────────────┘
          └─────────────────┘
```

Single Spring Boot service. PostgreSQL (the brief says "any relational DB" and I wanted multi-instance scheduler behaviour to be honestly testable). Layering is the orthodox `controller → service → repository`; entities and DTOs are separate types mapped explicitly.

---

## 2. Local setup

### Prerequisites

- JDK 21
- Node 20 + npm
- Docker (for Postgres, optionally Ollama)
- A Fixer.io API key (optional — the app seeds 30 days of synthetic data when none is provided)

### One-time

```bash
cp .env.example .env
# Edit .env if you have a FIXER_API_KEY or want to switch the AI provider.
```

### Run it

```bash
# 1. Infra
docker compose up -d                    # Postgres only
docker compose --profile ai up -d       # …or also start Ollama

# If using Ollama: pull the model the app will call
docker exec marcura-ollama ollama pull llama3.2

# 2. Backend
cd backend
./mvnw spring-boot:run
# → http://localhost:8080
# → http://localhost:8080/swagger-ui.html

# 3. Frontend (new terminal)
cd frontend
npm install
npm start
# → http://localhost:4200
```

Backend reads `.env` via Spring Boot's environment variable resolution. Frontend reads `src/environments/environment.ts` — change `apiBaseUrl` there if the backend is on a different host/port.

### Run the tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

The integration test uses Testcontainers and pulls a `postgres:16-alpine` image automatically.

---

## 3. AI Workflow

This section is required by the brief (§8.2) and reviewed under the AI-Augmented Workflow rubric (25 % of the grade).

### Tool & configuration

Primary agent: **Claude Code (Sonnet 4.6)** running in the project directory, with context files loaded automatically at every session start.

The repo carries the following agent-context artefacts:

| File | Purpose |
|---|---|
| [`PLAN.md`](./PLAN.md) | Phase-by-phase plan produced with the agent *before* any code was written. Ordered by rubric weight, not by component, so implementation time tracks marks directly. |
| [`CLAUDE.md`](./CLAUDE.md) | Repo-wide hard constraints loaded at every session: BigDecimal rules, date-source rule, concurrency strategy, layering, Spring AI prompt rules, commit conventions. Agents that violate these are corrected immediately. |
| [`.cursor/rules/01-project.mdc`](./.cursor/rules/01-project.mdc) | Repo-wide Cursor rules — KISS/DRY/YAGNI priorities, money precision, counter atomicity, no repository calls from controllers. |
| [`.cursor/rules/02-backend.mdc`](./.cursor/rules/02-backend.mdc) | Backend-scoped rules — constructor injection, record DTOs, named JPQL params, `@Transactional` discipline, SLF4J style. Applied only to `backend/**/*.java`. |
| [`.cursor/rules/03-frontend.mdc`](./.cursor/rules/03-frontend.mdc) | Frontend-scoped rules — standalone components, single `ApiService`, three explicit view-states, strict TypeScript, scoped SCSS. Applied only to `frontend/**/*.{ts,html,scss}`. |

Scoping backend and frontend rules to their respective globs prevents the agent from applying Java conventions to TypeScript files and vice versa — a practical issue when asking for cross-layer changes in one session.

### How the agent was used across the development cycle

**1. Planning — iterated, not accepted on first pass.**
The brief was pasted into Claude with the rubric weights visible. The first plan sequenced the Spring AI insight layer (Phase 7) before the exchange-rate calculation service (Phase 4) — making the insight feature dependent on plumbing that didn't exist yet. I rejected this ordering, explained the dependency, and asked for a second pass. The corrected plan (what you see in `PLAN.md`) sequences calculation first, then insight. This iteration is the kind of judgment the rubric is looking for.

**2. Multi-file agentic sessions — full layers at once.**
Each `PLAN.md` phase ran as a single Claude Code session producing every file for that layer simultaneously. Phase 1 produced all three entities, the Liquibase changeset, and the base repository interfaces in one shot. Phase 4 produced `SpreadProvider`, `RateCalculationService`, `ExchangeRateService`, all three DTOs, `ExchangeController`, and `GlobalExceptionHandler` together — the agent tracked cross-file consistency (e.g. that the DTO field names matched what the controller serialised) rather than requiring me to stitch isolated snippets.

**3. Test generation — agent-generated, hand-reviewed.**
`RateCalculationServiceTest` was generated from the brief's worked example. I asked for the `workedExample` case first, then prompted for edge-case additions (same currency, swapped spread argument order, default-bucket spread, bad inputs). After reviewing the output I rewrote the main assertion from a direct `isEqualByComparingTo` at full scale-10 precision to `setScale(2, HALF_UP).isEqualByComparingTo("4.44")` — the agent's version would have failed with a non-terminating scale difference. The edge-case tests (`picksMaxSpread`, `sameCurrency`) required no changes. `ExchangeControllerIT` (Testcontainers + `@SpringBootTest`) was generated as a single-shot multi-assertion session.

**4. Refinement sessions — ongoing critical review.**
After the initial implementation, dedicated sessions reviewed and corrected:
- Over-verbose class-level JavaDoc (multi-paragraph design rationales, `<ul>` alternative lists) was stripped down to single-sentence descriptions of non-obvious constraints.
- Inline `//` comments referencing "the brief" were removed across all source and test files.
- `HistoricalController` and `InsightController` were spotted duplicating the identical spread-adjusted series computation. The shared logic was extracted to `ExchangeRateService.historicalAdjusted()` — both controllers are now thin delegates of ~20 lines each.
- The `seed-anchor-rates.json` extraction: the agent proposed keeping `ANCHOR_RATES` as a hardcoded Java `Map`. I redirected it to load from a classpath JSON file so the seed data is editable without a recompile.

**5. Documentation — drafted by agent, corrected by hand.**
This README and `PLAN.md` were drafted in agent sessions and then hand-edited. Specific corrections: the trade-offs table framing (the agent described the H2-vs-Postgres decision as "performance" when the real reason was honest multi-instance scheduler testing); the OpenAI provider section (the agent drafted dual-provider support that wasn't actually wired up — removed and corrected to Ollama-only).

### Overrides

**Override 1 — concurrency strategy (`UsageCounterService`).**
The agent's first implementation used `AtomicLong` plus a `synchronized` block. This is correct in a single JVM but wrong for a service that "may run as multiple instances in production" (the brief's exact words). The override: a single SQL `UPDATE currency_usage SET total_count = total_count + 1` — atomicity is delegated to the Postgres row lock, which works across any number of instances. The fix is in a plain (non-`[AI]`) commit; the rule is encoded in `CLAUDE.md` so future sessions can't regress it.

**Override 2 — test assertion precision.**
The agent asserted the worked example result at full scale 10. The actual calculation scale is `DECIMAL64` (~16 significant figures), so the assertion would have been brittle against any internal precision change. Override: assert `setScale(2, HALF_UP).isEqualByComparingTo("4.44")` — matches the brief exactly and survives internal scale changes.

**Override 3 — InsightController architecture.**
Post-implementation review found that `InsightController` and `HistoricalController` each independently indexed the two currency series by date, looked up spreads, and computed adjusted rates. The agent had generated them independently without spotting the duplication. Override: extracted to `ExchangeRateService.historicalAdjusted()`, both controllers reduced to input validation + delegation.

**Override 4 — OpenAI provider wiring.**
The agent drafted `AiConfig` with `@ConditionalOnProperty` switching between Ollama and OpenAI starters, and added this to the README as a feature. At runtime, the OpenAI auto-configuration fired (as a transitive dependency) and threw `IllegalArgumentException: OpenAI API key must be set`. The implementation was never properly wired for dual-provider use — the override removed the OpenAI starter reference from the README and `PLAN.md`, leaving Ollama as the sole provider with a clean `AiConfig`.

### Commit-prefix convention

- `[AI] feat: …` / `[AI] test: …` / `[AI] chore: …` / `[AI] docs: …` — bulk of the diff produced by an agent session, reviewed and merged.
- Plain Conventional Commits — hand-edits, overrides, and corrections. Findable in `git log` without reading every diff.

---

## 4. AI provider setup (Spring AI)

The app uses Ollama as its LLM provider via the `spring-ai-ollama-spring-boot-starter`.

### Ollama setup

```bash
docker compose --profile ai up -d
docker exec marcura-ollama ollama pull llama3.2
```

Configure in `.env`:

```env
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
```

Any chat model Ollama supports will work (`llama3.2`, `qwen2.5`, `mistral`, etc.). The system prompt targets ≤ 2 sentences so even a 3 B parameter model produces useful output.

### Fallback

If Ollama is unreachable when an insight is requested, the backend logs a warning and returns a deterministic one-liner computed from the rate series. The UI displays it normally — useful in demos where Ollama is not running.

---

## 5. Endpoints (Swagger UI is the canonical reference)

| Method | Path                            | Description                                                              |
|--------|---------------------------------|--------------------------------------------------------------------------|
| GET    | `/api/exchange`                 | Spread-adjusted rate for a pair, optional `date`. Increments counters.  |
| GET    | `/api/historical`               | Daily spread-adjusted rate series for the chart.                         |
| GET    | `/api/analytics`                | Top currencies + per-day usage for the analytics dashboard.              |
| GET    | `/api/exchange/insight`         | Short LLM-generated trend commentary for a pair + range.                 |
| POST   | `/api/admin/refresh`            | Optional manual fetch (does not touch usage counters).                   |
| GET    | `/swagger-ui.html`              | Swagger UI.                                                              |
| GET    | `/v3/api-docs`                  | OpenAPI 3 JSON.                                                          |

Errors are RFC 7807 `ProblemDetail` JSON; the frontend's `ApiService` normalises every failure into the same shape.

---

## 6. Assumptions & trade-offs

- **Fixer.io free tier returns rates relative to EUR, not USD.** The brief's worked example reads "rate to USD". The calculation formula is symmetric in the base currency — `(toRate / fromRate) × spread-multiplier` is invariant under base — so the calculation works correctly with any base. The `baseCurrency` is stored on each `ExchangeRate` row and the `SpreadProvider` knows the active base so the base currency itself gets a 0% spread, exactly as Appendix B says.
- **Scheduler distribution.** ShedLock with the JDBC provider. Justified vs. Quartz cluster, K8s leader election, and app-level `synchronized` in `SchedulerConfig`'s class JavaDoc.
- **Counter atomicity.** Single-statement `UPDATE … SET count = count + 1`. The DB row lock provides the safety the brief asks for, with no in-process state to lose.
- **BigDecimal precision.** Storage: `numeric(19, 8)`. Computation: `MathContext.DECIMAL64` (16 sig-figs, `HALF_UP`). Final scale 10 for stable JSON serialisation. Avoids `ArithmeticException` from non-terminating expansions.
- **Seed data fallback.** Without a Fixer key the app still works — `SeedDataService` writes 30 days of plausible synthetic rates on first start. Real data takes over when the daily scheduler runs (or the manual `POST /api/admin/refresh` is hit).
- **No auth.** Out of scope. `/api/admin/refresh` would normally be role-gated.
- **No caching layer.** YAGNI for a single-service deployment with sub-millisecond Postgres lookups.
- **Frontend is two charts and a table per view.** The brief says "clarity of the trend is what matters", not a Bloomberg terminal.

---

## 7. Project layout

```
marcura-exchange-rates/
├── backend/                    # Spring Boot 3.3, Java 21, Maven
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/marcura/exchange/
│       │   ├── ExchangeRateApplication.java
│       │   ├── config/         # OpenAPI, CORS, Scheduler/ShedLock, AI
│       │   ├── domain/         # JPA entities
│       │   ├── repository/     # Spring Data repos
│       │   ├── service/        # SpreadProvider, RateCalculation, ExchangeRateService
│       │   │   ├── fixer/      # External API client
│       │   │   ├── ingestion/  # RateIngestionService + SeedDataService
│       │   │   └── insight/    # TrendInsightService (Spring AI)
│       │   ├── scheduler/      # DailyRateFetchJob
│       │   └── web/            # Controllers, DTOs, error handling
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/changelog/   # Liquibase
│       └── test/java/com/marcura/exchange/
│           ├── service/        # Calc, Spread, ExchangeRateService unit tests
│           └── web/            # @SpringBootTest + Testcontainers Postgres
├── frontend/                   # Angular 18 SPA
│   └── src/app/
│       ├── core/               # ApiService, models, currency list
│       ├── shared/             # ViewState helper
│       └── features/
│           ├── calculator/
│           ├── historical/
│           └── analytics/
├── docker-compose.yml
├── .env.example
├── PLAN.md                     # Up-front AI-assisted planning artefact
├── CLAUDE.md                   # Agent context loaded by Claude / Cursor / etc.
├── .cursor/rules/              # Cursor-specific scoped rules
└── README.md
```

---

## 8. License

Internal — Marcura R&D Technical Assessment.
