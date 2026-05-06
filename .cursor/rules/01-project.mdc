---
description: Marcura Exchange Rate System — repo-wide rules
globs: "**/*"
alwaysApply: true
---

# Repo-wide rules

Read `PLAN.md` and `CLAUDE.md` before any non-trivial change. The plan is ordered by rubric weight — understand the priorities before touching anything.

## Money — non-negotiable

- All rate values and computed results are `java.math.BigDecimal`. No `double`, no `float`, no `Number`. If you see one proposed, reject it immediately.
- DB scale: `numeric(19, 8)`. Computation scale: `MathContext.DECIMAL64`, final `setScale(10, HALF_UP)`. The constant is `RateCalculationService.RESULT_SCALE`.
- The formula is in `RateCalculationService.compute()`. Do not reimplement it inline anywhere else.

## Dates — non-negotiable

- `rateDate` stored on `ExchangeRate` is the date the **Fixer API reports** in the response body — never `LocalDate.now()`. This was an explicit brief requirement and the test in `ExchangeControllerIT` relies on it.
- All API date params are `LocalDate` with `@DateTimeFormat(iso = ISO.DATE)`.
- Scheduler timezone is `GMT` — the cron string must say `zone = "GMT"`, not `"UTC"`.

## Concurrency — non-negotiable

- Usage counters are incremented via `CurrencyUsageRepository.upsertIncrement()` — a single native SQL `INSERT … ON CONFLICT DO UPDATE SET count = count + 1`. The Postgres row lock provides atomicity across threads **and** instances.
- **Never propose `AtomicLong`, `synchronized`, or `ReentrantLock` for this counter.** The first agent draft used `AtomicLong` and was overridden. The rule is encoded here so it does not regress.

## Duplication rule — learned from experience

- Spread-adjusted series computation lives once in `ExchangeRateService.historicalAdjusted()`. Both `HistoricalController` and `InsightController` delegate to it.
- Before generating a new endpoint that needs rate data, check `ExchangeRateService` first. The first agent pass generated two controllers with identical index/spread/compute logic — this was caught and extracted. Don't repeat it.

## Layering

```
controller → service → repository → DB
                     └→ external client (FixerClient, ChatClient)
```

- Controllers implement a `*Api` interface from `web/api/`. They never call repositories directly.
- Services own `@Transactional` boundaries. DTOs are mapped in the service layer, not in controllers.
- Throw `RateNotFoundException` or `IllegalArgumentException`; `GlobalExceptionHandler` maps them to RFC 7807 `ProblemDetail`.

## Dependencies

Before adding any dependency, state the artifact ID, version, and why nothing already on the classpath covers the need. Every added jar is something the reviewer has to trust.

## Commits

- `[AI] feat/test/chore/docs: …` — agent produced the bulk of the diff.
- Plain Conventional Commits — hand-edits and overrides. These are the signal reviewers look for.
