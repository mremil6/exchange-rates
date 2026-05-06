# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project at a glance

Backend: Java 21 · Spring Boot 3.3 · Maven · Spring Data JPA · PostgreSQL 16 · Liquibase · Spring AI · ShedLock.
Frontend: Angular 18 · TypeScript strict · standalone components · ng2-charts · Reactive forms.
Infra: docker-compose for Postgres + (optionally) Ollama.

Always read `PLAN.md` and the brief in `docs/brief.pdf` before suggesting architecture changes.

---

## Commands

### Infrastructure

```bash
docker compose up -d                    # Postgres only
docker compose --profile ai up -d       # Postgres + Ollama
docker exec marcura-ollama ollama pull llama3.2   # Pull model (Ollama only)
```

### Backend

```bash
cd backend
./mvnw spring-boot:run                  # Run the application (port 8080)
./mvnw test                             # Run all tests
./mvnw -q package -DskipTests          # Build jar
./mvnw test -Dtest=ClassName            # Run a single test class
./mvnw test -Dtest=ClassName#methodName # Run a single test method
```

### Frontend

```bash
cd frontend
npm install
npm start                               # Dev server at http://localhost:4200
npm test -- --watchAll=false            # Run tests once (CI mode)
npx ng lint                             # Lint
```

### First-time setup

```bash
cp .env.example .env
# Edit .env to add FIXER_API_KEY or adjust OLLAMA_MODEL
```

Without `FIXER_API_KEY`, `SeedDataService` writes 30 days of synthetic rates on first boot — the app is fully demoable without a key.

---

## Architecture

### Key design decisions

- **Fixer.io returns EUR-relative rates**, not USD-relative. The formula `(toRate / fromRate) × spread` is base-invariant, so this is handled without conversion. `baseCurrency` is stored on each `ExchangeRate` row; the base currency itself gets 0 % spread per Appendix B.
- **Multi-instance scheduler**: `DailyRateFetchJob` uses `@SchedulerLock` (ShedLock JDBC provider) — at-most-once across instances, lease-based so a crashed node won't block forever, reuses the same Postgres schema (no extra infra).
- **AI provider (Ollama only)**: `AiConfig` exposes one `ChatClient` bean wired to `ollamaChatModel`. If Ollama is unreachable, `TrendInsightService` falls back to a deterministic one-liner computed from the rate series — the UI never shows a hard error.
- **Rate ingestion idempotency**: `RateIngestionService` upserts via `findByCurrencyAndRateDate` + save; catches `DataIntegrityViolationException` on race and retries. Stored `rateDate` is the date from the API response, not `LocalDate.now()`.
- **Seed anchor rates**: `SeedDataService` reads starting rates from `src/main/resources/seed-anchor-rates.json` (a `{ "CURRENCY": "rate" }` map). Edit that file to add or adjust seed currencies — no Java change required.
- **OpenAI auto-configuration suppression**: `application.yml` sets `spring.ai.openai.api-key: "not-used"`. Do not remove this line — the `spring-ai-spring-boot-autoconfigure` jar (pulled in transitively by the Ollama starter) registers OpenAI auto-configuration that throws `IllegalArgumentException` at startup if the key property is absent, even though OpenAI is never used.

### Backend package map

| Package | Responsibility |
|---|---|
| `domain/` | JPA entities: `ExchangeRate`, `CurrencyUsage` (no `CurrencyUsageDaily` — removed) |
| `repository/` | Spring Data repos, `@Query` with named params |
| `service/` | `SpreadProvider` (pure currency→spread map), `RateCalculationService`, `ExchangeRateService`, `AnalyticsService`, `UsageCounterService` |
| `service/fixer/` | `FixerClient` (RestClient), response DTOs |
| `service/ingestion/` | `RateIngestionService`, `SeedDataService` |
| `service/insight/` | `TrendInsightService` (Spring AI) |
| `scheduler/` | `DailyRateFetchJob` + ShedLock config |
| `web/` | Controllers, `dto/`, `error/GlobalExceptionHandler` |
| `config/` | CORS, OpenAPI, `AiConfig`, `SchedulerConfig` |

### Frontend structure

`core/services/api.service.ts` is the only file that calls `HttpClient`. Every method ends with `.pipe(catchError(this.normaliseError))` — components always receive a typed `ProblemDetail` on failure, never a raw `HttpErrorResponse`. Feature components (`calculator`, `historical`, `analytics`, `admin`) consume `ApiService` only. `core/models/exchange.models.ts` holds all shared TypeScript interfaces. `shared/view-state.ts` defines the `idle | loading | data | error` discriminated union used by all feature components.

---

## Non-negotiable conventions

### Money & rates
- All rate values, spreads, and computed exchange values are **`java.math.BigDecimal`** end-to-end.
- Persistence scale: `numeric(19, 8)`. Calculation scale: 10, `RoundingMode.HALF_UP`.
- Never use `double` or `float` for any monetary computation. If an agent ever proposes one, override.
- API responses serialise BigDecimal as a JSON number (Jackson default) — do not stringify.

### Dates
- All API dates are `LocalDate` in ISO `yyyy-MM-dd`.
- The stored rate date is **the date the API reports**, not `LocalDate.now()`. The brief is explicit.
- Scheduler timezone is `GMT` — the cron annotation must say `GMT`, not `UTC`.

### Concurrency
- The currency usage counter is incremented via `CurrencyUsageRepository.upsertIncrement()` — a single native SQL `INSERT … ON CONFLICT DO UPDATE SET count = count + 1`. No `synchronized`, no `AtomicLong`, no app-level lock.
- This delegates atomicity to the Postgres row lock — safe across threads and Spring Boot instances. If an agent proposes `AtomicLong`, override and explain. This rule is encoded here because the first agent draft used `AtomicLong` and was overridden.

### Layering
```
controller -> service -> repository -> DB
                      `-> external client (FixerClient, ChatClient)
```
Controllers never touch repositories directly. Services own transactions. DTOs live next to controllers, entities next to repositories — they are **separate types**, mapped explicitly in the service layer.

Every controller implements a `*Api` interface from `web/api/`. The interface carries all Swagger/OpenAPI annotations (`@Tag`, `@Operation`, `@RequestMapping`). The controller class carries only `@RestController`, `@RequiredArgsConstructor`, and `@Validated`.

`ExchangeRateService.historicalAdjusted()` is the single implementation of the spread-adjusted series computation. Both `HistoricalController` and `InsightController` delegate to it. Do not add a third caller that duplicates this logic — the first agent pass generated two controllers with identical index/spread/compute code and it was caught and extracted.

### Naming
- Controllers: `*Controller`, mapped under `/api/...`.
- Services: noun + `Service`. One responsibility. Constructor injection only — no `@Autowired` on fields.
- DTOs: `*Request` for inbound, `*Response` for outbound. Records when immutable.
- Repositories: `*Repository extends JpaRepository<Entity, Id>`. Custom queries via `@Query` with named parameters; no string concatenation.
- Logging: SLF4J `log.info("ingested {} rates", n)` — no `System.out`, no string concatenation.
- `@Transactional` on service methods that mutate state; `@Transactional(readOnly = true)` on reads.

### Errors
- Throw typed exceptions (`RateNotFoundException`, `InvalidCurrencyException`).
- `GlobalExceptionHandler` translates to `ProblemDetail` (RFC 7807).
- Don't return `ResponseEntity<String>` of hand-rolled error JSON.

### Tests
- JUnit 5 + AssertJ + Mockito.
- Coverage target: 100 % line coverage for the `service`, `scheduler`, and `web` packages.
- **4.44 vs 4.50 — know the difference.** `RateCalculationServiceTest.workedExample` asserts `4.44` using the brief's explicit spreads (EUR=1%, PLN=4%) passed directly to `RateCalculationService.compute()`. Any test that calls `ExchangeRateService.exchange("EUR","PLN",...)` with rates 0.8/3.7 must expect `4.50`, because the real `SpreadProvider` gives EUR=0% (base currency) and PLN=2.75% (default). These are two different things; do not conflate them.
- `test/resources/application.yml` must say `spring.liquibase.enabled: true`, not `spring.flyway.enabled` — the project uses Liquibase.
- `FixerClient` has a package-private constructor `FixerClient(RestClient, String)` for unit tests. The test class must live in `com.marcura.exchange.service.fixer` (same package) to access it.
- Controller tests use `@WebMvcTest(XxxController.class)` with `@MockBean` for every service dependency. Paths in MockMvc are the controller's `@RequestMapping` value without the `/api` servlet prefix — MockMvc bypasses the servlet container mapping.
- `GlobalExceptionHandler` is tested via `GlobalExceptionHandlerTest` (direct unit test) — instantiate it directly and call handler methods; no Spring context needed.
- `SeedDataService` field `seedDays` is `@Value`-injected and won't be set in unit tests. Use `ReflectionTestUtils.setField(service, "seedDays", N)` after construction.
- `ExchangeControllerIT` (`@SpringBootTest` + Testcontainers Postgres) covers the `/api/exchange` happy path and 404. It requires Docker — it is excluded from the standard `mvnw test` run when Docker is unavailable.

---

## Spring AI rules

- One `ChatClient` bean, wired to Ollama's chat model in `AiConfig`. No second bean, no `@ConditionalOnProperty` toggle.
- `TrendInsightService` depends on `ObjectProvider<ChatClient>`. `chatClientProvider.getIfAvailable()` returns `null` when Ollama is down — always check for null and use the deterministic fallback. Never let a null `ChatClient` propagate as a `NullPointerException`.
- The trend-insight prompt **must** inline the actual rate series as JSON in the user message — never describe the data in English.
- System prompt constants (`SYSTEM_PROMPT`) and per-request user message (`USER_PROMPT_TEMPLATE`) are kept as separate fields — do not collapse them into one string.
- System prompt constraints: ≤ 2 sentences, no markdown, no financial advice, mention direction + magnitude + timing.
- Temperature 0.2.
- If Ollama is unreachable, return the deterministic fallback — never propagate the exception to the client.

---

## Angular rules

- Standalone components only. No `NgModule`.
- A single `ApiService` with typed methods. No `HttpClient` calls outside it. Every method pipes through `catchError(this.normaliseError)`.
- API base URL from `environment.ts`, never hard-coded.
- Reactive forms (`FormBuilder`) for any form with more than one field. Built-in validators preferred; custom only when justified.
- `shared/view-state.ts` defines `ViewState<T>` — a discriminated union of `idle | loading | data | error`. Use `signal<ViewState<T>>(idle())` for components with a user trigger (button), `signal<ViewState<T>>(loading())` for components that load on `ngOnInit`.
- Each feature component renders all four states explicitly. No flash of empty UI, no silent null.
- When adding a new route, update three files in the same session: the component, `app.routes.ts` (lazy `loadComponent`), and the nav link in `app.component.html`. Partial wiring leaves a broken nav.
- Strict TS (`strict: true`, `noImplicitAny`, `noUncheckedIndexedAccess`). No `any` in committed code.
- Charts via `ng2-charts`. Keep chart configs in the component file, not a shared grab-bag.

---

## Commit conventions

- `[AI] feat: …`, `[AI] test: …`, `[AI] chore: …`, `[AI] docs: …` for any commit where an agent produced the bulk of the diff.
- Plain Conventional Commits (no `[AI]`) for hand-edits and AI **overrides** — this makes override-evidence findable in `git log`.
- Keep commits scoped — one commit per phase from `PLAN.md` is the target.

---

## Agent operating tips

- Before generating any new layer, list the files you plan to touch. The user will approve before you write.
- When in doubt about money, ask. When in doubt about dates, ask. When in doubt about anything else, prefer the simplest thing that satisfies the rubric.
- After any non-trivial change, run `./mvnw -q -DskipTests=false test` from `backend/` (or `npm test -- --watchAll=false` from `frontend/`).
- Don't add new dependencies without flagging them — every added jar is something the reviewer has to trust.
