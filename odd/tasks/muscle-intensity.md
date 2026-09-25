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
- Mobility: `MOBILITY` guarantees only a positive `durationSeconds`, so the stimulus is the normalized logarithmic duration `(1 + ln(1 + durationSeconds)) / (1 + ln(1 + maximumMobilityDurationInRange))`; a null or non-positive duration contributes `0.0` instead of a baseline.
- A set whose category is none of the three known modalities contributes `0.0` stimulus, never a baseline.
- Stage 1 (SQL): every modality is scaled against its own per-category maximum so strength volume, cardio duration/distance, and mobility duration stay comparable across modalities. Stage 2 (service): the query aggregates stimulus by muscle and impact level, the service multiplies each group by `ImpactLevel.weight()` and sums all groups, then normalizes the highest muscle to `10.0` so a score reads as "relative to my strongest muscle in the window". Both stages are required.
- Return `0.0` for all muscles when the maximum is zero. Round with `HALF_UP` to one decimal.

## Assumptions and documented limits

- `from` and `to` are inclusive calendar dates in the authenticated user's configured timezone; the SQL interval is `[from at start of day, to + 1 day at start of day)`.
- The date used for a completed session is `completed_at`, matching the existing historical-statistics convention. A completed session with no `completed_at` cannot be placed in a date range and is excluded.
- `Difficulty` is not used because its enum has no defined quantitative semantics.
- Impact weights are domain-level visualization weights, not scientific coefficients.
- A score of `10.0` means the highest-stimulus muscle in this selected range, not maximum physiological intensity; changing the range can change all scores.
- Missing strength weight does not erase a completed set; it removes only the load contribution.
- Test fixtures honour the `Category` validation rules: a `MOBILITY` exercise never carries reps, weight, or distance in repository or integration coverage.
- A set whose category is outside the three known modalities is only reachable by relaxing the `exercises` category constraint. `getMuscleIntensity_shouldIgnoreSetsOutsideKnownModalities` therefore inserts a domain-valid `STRENGTH` set (`reps=10`, `weightKg=50.0`, `durationSeconds=null`) and then flips the persisted category to `'CALISTHENICS'` inside the same rolled-back transaction, so no fixture row is unreachable through the API.
- That test asserts the persisted category is `'CALISTHENICS'` before reading the aggregate. The flip is a raw `UPDATE` that can silently match no rows, and without the premise assertion the test could pass without ever reaching the query's unknown-category branch.

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
- 2026-09-25 — strict GGA review-fix pass. All seven findings were fixed in one review unit covering the repository SQL, the service, and the three test layers:

  1. Unreachable mobility branch removed: the mobility stimulus now depends only on `durationSeconds`, the single metric the `MOBILITY` category guarantees. It stays logarithmically scaled and a null or non-positive duration contributes `0.0`, so no set can produce NaN or Infinity.
  2. Domain-invalid fixtures replaced: every mobility fixture now carries `reps = null`, `weightKg = null`, `distanceKm = null`, and a positive `durationSeconds`. The two mobility tests were rewritten as `getMuscleIntensity_shouldNormalizeMobilityDuration` (positive, finite, duration-scaled stimulus) and `getMuscleIntensity_shouldContributeZeroForMobilitySetWithoutDuration` (exactly `0.0`, finite). The sanctioned strength null/zero-weight and cardio null/zero-distance cases are unchanged.
  3. Two-stage normalization documented in the Javadoc of `StatsRepository.getMuscleIntensity` (stage 1: per-category maxima, keeps modalities comparable) and `StatsService.getMuscleIntensity` (stage 2: per-muscle 0-10 scaling, "relative to my strongest muscle in the window"), including that both stages are required and that the score is an application-level visualization heuristic.
  4. Hardcoded literals centralized: `'STRENGTH'`, `'CARDIO'`, `'MOBILITY'`, and `'COMPLETED'` are now bound as named parameters from `Category.STRENGTH/CARDIO/MOBILITY` and `SessionStatus.COMPLETED`. The silent `ELSE 1.0` is gone; an unrecognized category contributes `0.0`.
  5. Error semantics corrected: `getExerciseProgress` throws `BadRequestException("The start date cannot be after the end date")` for an inverted range, matching the documented 400 contract and the message already used by muscle-intensity range validation.
  6. Unused `@Slf4j` annotation and import removed from `StatsService`; no logging was added.
  7. `StatsRepositoryTest.persistSet` now returns the imported `SessionSet` instead of the fully qualified name.

  RED evidence before the production fix:

  - `StatsRepositoryTest > Should contribute zero stimulus for mobility sets without a positive duration` — `expected: 0.0 but was: 2.0` (one `ELSE 1.0` baseline per duration-less set).
  - `StatsRepositoryTest > Should contribute zero stimulus for a category outside the known modalities` — `expected: 0.0 but was: 1.0` (the silent unknown-category baseline).
  - `StatsServiceTest > Should reject progress when start date is after end date` — expected `BadRequestException`, got `ResourceNotFoundException: Exercise progress not found`.
  - `StatsControllerIT > Should return 400 when the progress range is inverted` — `Status expected:<400> but was:<404>`.

  GREEN evidence after the production fix:

  - `./gradlew spotlessApply` — `BUILD SUCCESSFUL`; the formatter required no changes to the edited files.
  - `./gradlew test --tests '...unit.StatsServiceTest' --tests '...repository.StatsRepositoryTest' --tests '...integration.StatsControllerIT'` — `BUILD SUCCESSFUL`, 52 tests, 0 failures, 0 errors, 0 skipped.
  - `./gradlew build` — `BUILD SUCCESSFUL`, 53 test classes, 288 tests, 0 failures, 0 errors, 0 skipped. The only compiler warning is the pre-existing `ProgressRecordMapper` unmapped-target-properties warning, unrelated to this feature.

- 2026-09-25 — second GGA review-fix pass. One blocking finding and three import-hygiene nits were fixed. No production code changed, and no test was removed or weakened.

  Blocking finding — `getMuscleIntensity_shouldIgnoreSetsOutsideKnownModalities` built the exercise as `Category.MOBILITY` and then persisted a set with `reps=10` and `weightKg=50.0`. `Category.MOBILITY.validate()` requires `reps == null` and `weightKg == null`, and `WorkoutSessionService` enforces that on every set write, so the row was unreachable through the API. The other mobility fixtures had already been corrected; this one was missed. Fix, following the reviewer's prescription:

  1. The exercise is now created as `Category.STRENGTH`, whose validation accepts `reps=10`, `weightKg=50.0`, `durationSeconds=null`, `distanceKm=null`.
  2. The set is persisted with exactly those metrics, so every fixture row in the test is now domain-valid.
  3. Both existing constraint drops are kept (`exercises_category_check` for the Hibernate `create-drop` test schema, `ck_exercises_category` for the Flyway `V4` migration) so the `UPDATE` is permitted; Postgres DDL is transactional and the rollback restores them.
  4. A comment records why the insert-then-flip shape is the only way to stage an out-of-domain category.
  5. The test now asserts the persisted category is `'CALISTHENICS'` before reading the aggregate, so it proves its own premise. The existing stimulus assertions (exactly `0.0`, finite, single element for that muscle) are unchanged.

  Proof that the premise assertion is load-bearing, not decorative: with the flip temporarily pointed at a random id (matching no rows), the test failed with `expected: "CALISTHENICS" but was: "STRENGTH"`. The mutation was reverted and the suite re-run green. This matters because the corrected fixture leaves `durationSeconds=null`; had the flip silently no-opped, the row would have stayed `STRENGTH` and scored about `1 + ln(11)`, so the stimulus assertion now also fails independently.

  Import hygiene, in the same pass:

  - Correction to the first pass: its finding 7 claimed the fully qualified name class of issue was fixed, and that was not true. It only replaced the qualified `SessionSet` return type of `StatsRepositoryTest.persistSet`. Seven inline fully qualified references survived in the same file, and `palantirJavaFormat` does not reject them, so `spotlessCheck` never flagged them.
  - `StatsRepositoryTest` — `new dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection(50.0, 10)` became `new VolumeSetProjection(50.0, 10)` with a normal import.
  - `StatsRepositoryTest` — five `org.assertj.core.data.Offset.offset(1e-12)` references became `offset(1e-12)` with `import static org.assertj.core.data.Offset.offset;`, added to the existing static-import group. A plain `import ...Offset` would have been an unused import, since the reference becomes a bare static call.
  - `StatsServiceTest` — `java.util.Set.of(exerciseId)` became `Set.of(exerciseId)`. Note the first pass's description was wrong in a small way: that file did not already import `java.util.Set`, so the import was added rather than reused.
  - Both files were re-checked for duplicate and unused imports after the edits; every imported name is referenced.

  Second-pass verification:

  - `./gradlew spotlessApply` — `BUILD SUCCESSFUL`; the formatter kept the import placement chosen for the new imports.
  - `./gradlew test --tests '...unit.StatsServiceTest' --tests '...repository.StatsRepositoryTest' --tests '...integration.StatsControllerIT' --rerun-tasks` — `BUILD SUCCESSFUL`, 52 tests, 0 failures, 0 errors, 0 skipped.
  - `./gradlew build --rerun-tasks` — `BUILD SUCCESSFUL`, 53 test classes, 288 tests, 0 failures, 0 errors, 0 skipped, matching the pre-review baseline.

## Verification evidence

- `./gradlew spotlessApply` — passed; formatter made no changes.
- Focused `StatsServiceTest`, `StatsRepositoryTest`, and `StatsControllerIT` — passed.
- `./gradlew build` — passed, including compilation, tests, Spotless check, packaging, and check tasks.
- GGA review of the data-model slice — provider returned `STATUS: PASSED`; the surrounding commit was blocked by generated-file staging/index corruption, not a code-review finding.
- 2026-09-25 review-fix pass — `./gradlew spotlessApply`, the three focused test classes (52 tests), and `./gradlew build` (288 tests) all passed; see the Progress entry for the RED evidence captured before the fix.
- 2026-09-25 second review-fix pass — `./gradlew spotlessApply`, the three focused test classes with `--rerun-tasks` (52 tests), and `./gradlew build --rerun-tasks` (288 tests) all passed. All three were re-run after the temporary no-op-flip mutation was reverted, so the reported results belong to the delivered state.

## Commit evidence

- `7cbd93a chore: add repository review rules` — committed successfully.
- Remaining production and test slices are pending commit after the generated-file staging issue is isolated.
- 2026-09-25 review-fix pass — the work-unit commit is intentionally **pending**: the orchestrator owns the GGA-backed commit, and no commit was created from this session. The reviewed unit is the staged feature slice plus these review fixes.
- 2026-09-25 second review-fix pass — still **pending** for the same reason; no commit was created and no hook bypass was used. The orchestrator owns the GGA-backed commit of the staged feature slice plus both review-fix passes.
- 2026-09-25 — commit deliberately **not** created. Two GGA passes reviewed this unit: the first returned `STATUS: PASSED`, the second returned `STATUS: FAILED` with one blocking finding and three import nits, all fixed and re-verified afterwards. The remaining blocker is not a code finding: GGA 2.10.1 only looks for the `STATUS:` line in the first 30 lines of provider output, while the OpenCode agent emits its full tool trace first, so the verdict landed at line 1798 of 1825 and was rejected as "ambiguous response" — including the passing verdict. Maintainer decision: keep `STRICT_MODE="true"`, do not bypass the hook, and do not relax the guard. The unit stays staged and uncommitted.

## Next step

The muscle-intensity unit is complete and verified but cannot land through the current GGA setup: the strict guard rejects the provider's verdict because it arrives past GGA's 30-line status window, and the maintainer chose to keep the guard strict rather than bypass it or set `STRICT_MODE="false"`.

Land it when either of these happens:

1. GGA parses an OpenCode verdict that arrives late in the output (upstream fix, or a provider/agent profile whose response leads with the status line), then run the GGA-backed work-unit commit and verify the final repository status.
2. The maintainer approves a different delivery route for this already-reviewed unit.

Delivery note: the maintainer approved a one-work-unit commit under a `size:exception` (1,023 authored lines) rather than chained PRs, so no commit slicing is pending.

Unrelated pre-existing risks surfaced by the review, deliberately left out of this change:

- `StatsRepository.java:112` — `rs.getTimestamp("completed_at").toInstant()` throws NPE → HTTP 500 when a session is `COMPLETED` with `completed_at IS NULL`, which migration `V9__create_workout_sessions.sql` permits. The muscle-intensity query excludes those rows by its range predicate, so `/streak` and `/muscle-intensity` disagree. Worth a follow-up.
- `StatsRepository.java:366` — `BodyRegion.valueOf(...)` throws `IllegalArgumentException` → HTTP 500 on an unexpected DB value; `muscles.body_region` has no CHECK constraint, unlike `impact_level`.
