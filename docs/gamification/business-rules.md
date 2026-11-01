# Business Rules and Calculation Semantics

## Completed Data

- Only completed sets can set a personal record.
- Only completed sessions count towards the threshold achievements.
- The evaluation compares a set against the records the user already holds,
  including the ones set earlier in the same session.

## Threshold Rules

- A threshold rule is pure data: a metric, an operator (`GTE` or `GT`), and a
  threshold. A new achievement of this kind is added by inserting a row in
  `achievement_definitions`, without a deployment.
- The metrics available to a threshold rule are the training aggregates:
  `TOTAL_WORKOUTS`, `LONGEST_STREAK`, `TOTAL_SETS`, `TOTAL_REPS`,
  `TOTAL_VOLUME_KG`, `TOTAL_TRAINING_SECONDS`, `TOTAL_DISTANCE_KM`, and
  `DISTINCT_EXERCISES`.
- `TOTAL_VOLUME_KG` is the sum of `weightKg * reps`, so a set without weight or
  without repetitions contributes nothing to it.
- The streak used by the streak achievements is the **longest** streak, not the
  current one: a progress value never goes backwards, and the current streak
  drops to zero when a day is missed.

## Personal Record Rules

- A record rule is evaluated in code, because comparing against the previous
  best needs the value of the record and the granularity it is kept at.
- The metrics available to a record rule are `MAX_WEIGHT`, `MAX_ESTIMATED_1RM`,
  `MAX_REPS_AT_WEIGHT`, `MAX_DISTANCE`, and `MAX_DURATION`.
- A record must be **beaten**, not tied: a value equal to the record does not
  unlock anything.
- The weight records need both the weight and the repetitions, because the
  estimated 1RM is derived from them. The distance and the duration stand on
  their own.
- `MAX_REPS_AT_WEIGHT` is kept per weight, so two weights of the same exercise
  are two different records.
- The estimated 1RM uses the Epley formula: `weightKg * (1 + reps / 30.0)`.

## Scope

- A global achievement produces one badge for the user.
- An achievement scoped to an exercise produces one badge per exercise. The
  catalog therefore lists a global achievement once, and a scoped achievement
  once for each exercise the user earned it on.
- The current value of a scoped achievement comes from the personal records, so
  it stays accurate even when the training that set it is deleted.

## Monotonic State

- The progress of an achievement never decreases.
- An unlocked achievement is never revoked. The achievement already happened;
  deleting the session that produced it does not unearn it.
- The records are recalculated when a destructive operation invalidates them, so
  the current value of a record reflects the training that actually exists.
- The threshold achievements are not recalculated after a deletion: their
  progress cannot go down and their unlock cannot be revoked, so the
  recalculation could not change anything.

## Triggering

| Event | Effect |
|---|---|
| A session is completed | Threshold achievements are evaluated, and the records of the session are evaluated |
| A session is created already completed | Same as a completed session |
| A set is completed | The records of the exercise are evaluated, so the user learns about a new record while the session is open |
| A set changes its metrics, is uncompleted, or is deleted | The records of the exercise are recalculated |
| A session exercise is deleted | The records of the exercise are recalculated |
| A session is deleted | The records of every exercise of the session are recalculated |

## Persistence

- The writes are idempotent. The same event twice leaves one row, with the best
  progress and the first unlock instant.
- The records keep pointing at the training that set the best value. A lower
  value never replaces the source of the record.
- The evaluation reads the training history with its own SQL, so the module that
  triggers it flushes its transaction before the evaluation runs. Otherwise the
  evaluation would read the state before the change.

## Streaks and Timezones

- The training days are converted to the user's IANA timezone before they are
  deduplicated.
- The longest streak is the longest run of consecutive local days, and it is
  retained even when the current streak has expired.
