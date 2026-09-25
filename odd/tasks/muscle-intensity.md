# Muscle Intensity Stats

## Objective

Implement `GET /api/v1/stats/muscle-intensity?from=YYYY-MM-DD&to=YYYY-MM-DD` for the authenticated user, returning a deterministic, range-relative 0–10 application heuristic for every muscle in the catalog.

## Problem

The frontend needs a body-map visualization, but raw kilogram volume is not comparable across exercises and can let one unusually heavy set dominate the result. The domain has no relative-load, RPE, fatigue, effort, or recovery measure.

## Scope

- Add a user-scoped database aggregation over `workout_sessions -> session_exercises -> session_sets -> exercise_muscles -> muscles`.
- Include only completed sessions owned by the authenticated user, sessions whose `completed_at` falls within the inclusive user-local date range, and completed sets.
- Add strength, cardio, and mobility heuristic handling with safe null handling and no cross-modality unit conversion.
- Centralize impact weights: `PRIMARY=1.0`, `SECONDARY=0.5`, `STABILIZER=0.25`.
- Return every muscle, including zero-stimulus muscles, with metadata and one-decimal intensity.
- Document the metric as a relative application heuristic, not physiological muscle activation.
- Add focused service, repository, and controller tests for the requested behavior.

## Out of scope

- New physiological concepts or fields such as RPE, RIR, fatigue, effort, recovery, or 1RM for this metric.
- Changes to unrelated workout, routine, exercise, or progress-record behavior.
- A new architectural layer or pagination for this small body-map result.

## Heuristic

For each qualifying completed set, `setFactor=1.0`.

- Strength: `repFactor = 1 + ln(1 + max(reps, 0))`; when `reps` and `weightKg` are positive, `volume = reps * weightKg` and `loadFactor = (1 + ln(1 + volume)) / (1 + ln(1 + maximumVolumeInRange))`. Missing/non-positive weight uses `loadFactor=1.0`.
- Cardio: use the greater of normalized logarithmic duration and normalized logarithmic distance when either is present; otherwise use the completed-set baseline. No seconds-to-kilometres conversion.
- Mobility: use `1 + ln(1 + max(reps, 0))` and normalized duration when duration is present; otherwise use the completed-set baseline.
- The query aggregates stimulus by muscle and impact level. The service multiplies each group by `ImpactLevel.weight()` and sums all groups.
- Normalize the highest muscle raw stimulus to `10.0`; return `0.0` for all muscles when the maximum is zero. Round with `HALF_UP` to one decimal.

## Assumptions and documented limits

- `from` and `to` are inclusive calendar dates in the authenticated user's configured timezone; the SQL interval is `[from at start of day, to + 1 day at start of day)`.
- The date used for a completed session is `completed_at`, matching the existing historical-statistics convention. A completed session with no `completed_at` cannot be placed in a date range and is excluded.
- `Difficulty` is not used because its enum has no defined quantitative semantics.
- Impact weights are domain-level visualization weights, not scientific coefficients.
- A score of `10.0` means the highest-stimulus muscle in this selected range, not maximum physiological intensity; changing the range can change all scores.
- Missing strength weight does not erase a completed set; it removes only the load contribution.

## Authorized scope and routes

- Implementation is explicitly authorized by the user's request.
- Route: delegated direct implementation, because the change spans repository SQL, service normalization, DTOs, controller wiring, and multiple test files.
- Mapping trigger: the initial exploration required the stats layer, workout graph, schema, error conventions, and tests (more than four files). A mapper delegation was attempted but the configured provider rejected the free-tier launch; exploration was completed with targeted repository reads instead.
- Writer trigger: one bounded writer will implement the production/test slice to avoid concurrent edits.
- Advisory delivery heuristic: this is a coherent API feature; no artificial line-count reduction or test omission is planned.

## TDD and checks

- Strict TDD is active from the project configuration: RED before implementation, GREEN, then REFACTOR.
- Test runner: `./gradlew test`.
- Full build/check command: `./gradlew build`.
- Formatting command: `./gradlew spotlessApply` before final verification, followed by `./gradlew spotlessCheck`.

## Tasks

- [x] T1 Define projection, centralized impact weights, and SQL-side modality stimulus aggregation.
- [x] T2 Add service normalization, date validation/timezone conversion, DTOs, and controller endpoint documentation.
- [x] T3 Add unit tests for zero handling, weighting, normalization, null weight, modality formulas, rounding, and invalid ranges.
- [x] T4 Add repository/integration coverage for filtering, boundaries, ownership, zero muscles, multiple contributions, cardio/mobility, and extreme volume.
- [x] T5 Run formatter, focused tests, full build, and inspect the final diff for unrelated changes.

## Acceptance criteria

- [x] Endpoint requires authentication and scopes every aggregate to `principal.id`.
- [x] Completed-session and completed-set filters are enforced in SQL.
- [x] Date boundaries are inclusive for `from` and `to` in the user's timezone.
- [x] All muscles are returned exactly once, including zero-stimulus muscles.
- [x] Multiple exercises and impact levels accumulate for the same muscle.
- [x] Maximum nonzero muscle score is `10.0` after one-decimal rounding.
- [x] Strength load is logarithmically bounded and missing/null weight is safe.
- [x] Cardio and mobility use available domain metrics without invented conversions.
- [x] Requested tests pass; any unavailable checks are reported explicitly.
- [x] No unrelated functionality is modified.

## Progress

- TDD mode: strict (`sdd-init/fitness-tracker-api` snapshot; runner `./gradlew test`).
- Route: delegated direct implementation; the change spans SQL, service normalization, DTOs, controller wiring, and three test layers.
- Production and test implementation is complete. The repository GGA hook is currently being handled in smaller work-unit commits because its provider times out on the full change set.

## Verification evidence

- `./gradlew spotlessApply` — passed; formatter made no changes.
- Focused `StatsServiceTest`, `StatsRepositoryTest`, and `StatsControllerIT` — passed.
- `./gradlew build` — passed, including compilation, tests, Spotless check, packaging, and check tasks.
- GGA review of the data-model slice — provider returned `STATUS: PASSED`; the surrounding commit was blocked by generated-file staging/index corruption, not a code-review finding.

## Commit evidence

- `7cbd93a chore: add repository review rules` — committed successfully.
- Remaining production and test slices are pending commit after the generated-file staging issue is isolated.

## Next step

Clean generated tooling artifacts from the index, commit the remaining work in reviewable slices, and verify the final repository status.
