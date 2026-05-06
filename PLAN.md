# Implementation Plan — Exchange Rate Management System

> Produced with AI assistance (Claude) before any implementation began.
> Owner: Senior Full Stack Engineer.
> Goal: ship a runnable, testable end-to-end system that maps cleanly to the brief's grading rubric.

## 1. Reading the brief through the rubric

The rubric (Section 10 of the brief) is what gets marked, so the plan is organised around it rather than around an arbitrary "MVP first" split. Weights:

| Area                  | Weight | Driving question                                                                 |
|-----------------------|--------|----------------------------------------------------------------------------------|
| Backend               | 25 %   | Is the API correct, idiomatic Spring, safe under concurrency, and precise?       |
| Frontend              | 20 %   | Three views, typed, validated, with clear UX states.                             |
| AI trend insight      | 20 %   | Spring AI wired correctly, prompt receives real numbers, output is constrained.  |
| AI-augmented workflow | 25 %   | Plan, config, multi-step agent use, evidence of overriding the AI.               |
| Overall engineering   | 10 %   | Swagger, tests, README that actually works.                                      |

The 25 % AI-workflow weight matters: even a perfect app with no workflow evidence caps at 75 %. So this plan, the `CLAUDE.md`, and the AI-prefixed commit history are first-class deliverables, not garnish.

## 2. Architecture at a glance

```
+---------------------------+        +---------------------------+
|  Angular 18 SPA           |  HTTP  |  Spring Boot 3.3 (Java 21)|
|  - Calculator             | <----> |  - REST controllers       |
|  - Historical + Chart     |        |  - Services + JPA repos   |
|  - Analytics              |        |  - Scheduler (ShedLock)   |
+---------------------------+        |  - Spring AI ChatClient   |
                                     +-------------+-------------+
                                                   |
                          +------------------------+------------------------+
                          |                        |                        |
                  +-------v-------+        +-------v-------+        +-------v-------+
                  | PostgreSQL 16 |        |  Fixer.io API |        | Ollama         |
                  | (rates + usage|        |  (daily fetch)|        |  (local LLM)   |
                  |  + shedlock)  |        +---------------+        +---------------+
                  +---------------+
```

A single Spring Boot service. No microservices — YAGNI. PostgreSQL because the brief says "any relational DB" and the rubric explicitly assesses multi-instance scheduler correctness, which is more honest with a real DB than with H2.

## 3. Sequenced work breakdown

The order matters: each step unlocks demoability of the next without leaving broken intermediate states.

### Phase 0 — repo skeleton & AI config (≈30 min)
- `README.md`, `PLAN.md`, `CLAUDE.md`, `.cursor/rules/*.mdc`, `.gitignore`, `docker-compose.yml`, `.env.example`.
- Empty `backend/` Maven project that compiles.
- Empty Angular workspace that runs `ng serve`.
- **Commit:** `[AI] chore: scaffold repo and agent config`.

### Phase 1 — domain + persistence (rubric: data persistence 6 %)
- Entities: `ExchangeRate(currency, rateDate, rateToBase)` with unique `(currency, rateDate)`; `CurrencyUsage(currency, totalCount, lastQueriedAt)`; ShedLock table.
- `BigDecimal(19, 8)` for rate values — enough precision for the worked example and any reasonable inverse.
- Liquibase changeset `001-init.sql`.
- **Commit:** `[AI] feat(domain): rate + usage entities, V1 migration`.

### Phase 2 — Fixer client + ingestion (rubric: data persistence, scheduler)
- `FixerClient` over Spring's `RestClient`. Typed response DTOs.
- `RateIngestionService.ingestForDate(LocalDate)`:
  - Calls Fixer `/latest` (or `/{date}` for backfill).
  - **Stores `response.date` from the API**, not `LocalDate.now()`. The brief is explicit about this and the rubric calls it out.
  - Upsert via JPA's `findByCurrencyAndRateDate` + save (idempotent on the `(currency, rateDate)` unique constraint, retried on `DataIntegrityViolationException`).
- `SeedDataService` populates 30 days of plausible synthetic rates if `FIXER_API_KEY` is unset, so reviewers without a key still get a fully demoable app.
- **Commit:** `[AI] feat(ingestion): fixer client + idempotent upsert + seed fallback`.

### Phase 3 — scheduler (rubric: scheduler correctness under multi-instance)
- `@Scheduled(cron = "0 5 0 * * *", zone = "GMT")` triggers `RateIngestionService`.
- Wrapped in `@SchedulerLock(name="dailyFetch", lockAtMostFor="PT10M", lockAtLeastFor="PT1M")` via [ShedLock](https://github.com/lukas-krecan/ShedLock) backed by JDBC.
- Justified in README: at-most-once across instances, lease-based so a crashed node won't block forever, no extra infra (uses the same Postgres).
- **Commit:** `[AI] feat(scheduler): daily fetch with shedlock for multi-instance safety`.

### Phase 4 — calculation + exchange API (rubric: core API 8 %, code quality 6 %)
- `SpreadProvider`: pure function over `Currency → BigDecimal` from Appendix B. Unit-tested.
- `RateCalculationService.adjustedRate(from, to, fromRate, toRate)` implements the formula with `BigDecimal` and a documented rounding mode (`HALF_UP`, scale 10). Worked example becomes a `@ParameterizedTest`.
- `ExchangeRateService.exchange(from, to, dateOpt)`:
  - Loads rates by date (or "max date available").
  - 404 (`RateNotFoundException`) when the requested date has no rates.
  - Calls `UsageCounterService.recordUsage(from, to)`.
- `GET /api/exchange?from=EUR&to=PLN[&date=YYYY-MM-DD]` → response shape from Appendix A.
- **Commit:** `[AI] feat(api): /exchange with spread-adjusted formula and 404 semantics`.

### Phase 5 — concurrency-safe counter (rubric: concurrency 5 %)
- `UsageCounterService.recordUsage(...)` uses a single SQL `UPDATE currency_usage SET total_count = total_count + 1, last_queried_at = :ts WHERE currency = :c`. If 0 rows, INSERT then retry the UPDATE.
- This delegates atomicity to the DB row lock — safe across threads **and** across Spring Boot instances. No `synchronized`, no app-level lock that would silently break under horizontal scaling.
- README will explain why this beats `AtomicLong` (in-process only) and why it beats `@Transactional(SERIALIZABLE)` (heavy + deadlock-prone).
- **Commit:** `[AI] feat(usage): atomic counter via UPDATE..SET count+1`.

### Phase 6 — historical + analytics endpoints (rubric: 6 %)
- `GET /api/historical?from=…&to=…&fromDate=…&toDate=…` → array of `{date, rate}` for the calculator pair.
- `GET /api/analytics` → `topCurrencies[]` plus per-day query counts.
- **Commit:** `[AI] feat(api): historical + analytics endpoints`.

### Phase 7 — Spring AI insight (rubric: AI integration 20 %)
- `spring-ai-bom` + `spring-ai-ollama-spring-boot-starter`.
- `TrendInsightService.generate(from, to, fromDate, toDate)`:
  - Loads historical rates for that window.
  - Builds a system prompt + user prompt where the rate series is **inlined** (not a description) — JSON list of `{date, rate}`.
  - Constraints in system prompt: ≤ 2 sentences, factual, mention direction/magnitude/timing, no financial advice, no markdown.
  - Calls `ChatClient` with low temperature (0.2) for stable phrasing.
- `GET /api/exchange/insight?from=…&to=…&fromDate=…&toDate=…` → response from Appendix A.
- README documents `ollama pull llama3.2` setup so reviewers can run it.
- **Commit:** `[AI] feat(insight): spring-ai chat client with constrained trend prompt`.

### Phase 8 — Angular SPA (rubric: 20 %)
- Angular 18, standalone components, signals + RxJS where appropriate.
- `core/services/api.service.ts` is the only place that touches `HttpClient`. Typed models.
- Three feature components: `calculator`, `historical`, `analytics`.
- Charts via Chart.js (`ng2-charts`) — minimal, line + bar.
- Reactive forms with built-in validators; `loading | error | data` view-model pattern.
- API base URL from `src/environments/environment.ts` so `ng serve` works against any backend.
- **Commit:** `[AI] feat(ui): three views with typed API service and reactive forms`.

### Phase 9 — Swagger + global error handling (rubric: 3 %)
- `springdoc-openapi-starter-webmvc-ui` → `/swagger-ui.html`.
- `@ControllerAdvice GlobalExceptionHandler` returns RFC 7807 `ProblemDetail` for 404, 400, 500.
- **Commit:** `[AI] chore: openapi + problem-detail error handler`.

### Phase 10 — tests (rubric: 4 %)
- Coverage target: 100 % line coverage for `service/`, `scheduler/`, and `web/` packages.
- Unit tests: `SpreadProviderTest`, `RateCalculationServiceTest`, `ExchangeRateServiceTest`, `AnalyticsServiceTest`, `UsageCounterServiceTest`, `TrendInsightServiceTest`, `RateIngestionServiceTest`, `FixerClientTest`, `SeedDataServiceTest`.
- Scheduler: `DailyRateFetchJobTest`.
- Web layer: `HistoricalControllerTest`, `InsightControllerTest`, `AnalyticsControllerTest`, `AdminControllerTest` via `@WebMvcTest`; `GlobalExceptionHandlerTest` as a direct unit test.
- Integration: `ExchangeControllerIT` with `@SpringBootTest` + Testcontainers Postgres.
- Key correction applied during test phase: `ExchangeRateServiceTest` and `ExchangeControllerIT` originally asserted `4.44` (copied from the brief's formula demo). Real `SpreadProvider` values (EUR=0% base, PLN=2.75% default) produce `4.50` for the 0.8/3.7 rate pair. Corrected to `4.50`. `RateCalculationServiceTest.workedExample` stays at `4.44` — it passes explicit spreads directly and is testing the formula, not the spread table.
- `test/resources/application.yml` uses `spring.liquibase.enabled: true` (not Flyway).
- **Commit:** `[AI] test: full coverage for service, scheduler, and web packages`.

### Phase 11 — README polish + screen recording script (rubric: 3 %)
- Setup, architecture, AI Workflow section (mandatory under §8.2.3 of the brief), assumptions, trade-offs.
- A short `RECORDING.md` outlining the 4-minute walkthrough so the recording stays focused.

## 4. Risks and the trade-offs I'm consciously making

| Risk                                                              | Decision                                                             | Why                                                                              |
|-------------------------------------------------------------------|----------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Fixer free tier returns rates relative to **EUR**, not USD.       | Convert to USD-based rates on ingest by dividing by USD's EUR-rate.  | The formula assumes "rate to USD". Documenting this in README under assumptions. |
| Reviewer doesn't have Ollama installed.                           | Deterministic fallback in `TrendInsightService` + clear UI message.  | Not failing the AI section because of the reviewer's environment.                 |
| Reviewer doesn't have a Fixer key.                                | Seed-data fallback (30 days of synthetic but plausible rates).       | App is fully demoable end-to-end with `docker compose up` and nothing else.       |
| Multi-instance scheduler is hard to truly verify in an assessment.| ShedLock + JDBC table + clear justification in README.               | The rubric says "approach and justification matter more than the specific mechanism".|
| BigDecimal scale issues on division.                              | Fixed scale 10, `HALF_UP`. Worked example asserts to scale 10.        | Avoids `ArithmeticException` from non-terminating expansions. Documented.         |
| Over-engineering Spring AI (RAG, embeddings).                      | Plain ChatClient with inlined data.                                  | Brief explicitly says "we are not expecting a production-grade RAG pipeline".     |

## 5. What "done" looks like

- `docker compose up` starts Postgres (and optionally Ollama).
- `cd backend && ./mvnw spring-boot:run` boots the API; ingest runs immediately on first start so there's data.
- `cd frontend && npm install && npm start` boots the SPA at `localhost:4200`.
- All three views demo-able. Insight panel populates. Swagger reachable.
- `./mvnw test` is green.
- Repo has commits prefixed `[AI]` for AI-assisted work and unprefixed for manual overrides (so the override-evidence the rubric wants is real, not narrated).

