# Business Rules and Implicit Behavior

## Session Lifecycle

- Sessions created from an active routine start as `IN_PROGRESS`; scratch-session requests must provide a status.
- Completing a session sets status to `COMPLETED` and overwrites `completedAt` with the current instant.
- Notes, exercise, and set mutations are rejected after completion.
- Deleting a session cascades to its exercises and sets.

## Position and Number Normalization

- Exercise positions are one-based.
- A null insertion position appends.
- Requested exercise positions are clamped to `[1, exerciseCount + 1]`; siblings are shifted.
- Moving an exercise clamps to the valid range and shifts affected siblings.
- Deleting an exercise closes the position gap.
- Set numbers supplied when adding/updating an existing set are ignored; the new set receives the next sequential number.
- Deleting a set closes the set-number gap. Deleting the sole set removes the parent session exercise.

## Defaults and Batching

- When creating from a routine, routine exercise metrics and default set counts are copied.
- When creating from scratch with an exercise whose set list is empty, one set is synthesized.
- Synthesized values reuse the user's last completed set for that exercise when available; otherwise category defaults are used.
- The last-set defaults for all exercises in a scratch-session request are loaded with one batch repository query, not one query per exercise.
- Session response mapping calculates achievements in one batch for all completed sets in the session. Individual set responses calculate achievements only when the set is completed.

## Validation and Ownership

All resources are user-scoped. Active exercises and active routines are required when creating from references. Category validation rejects incompatible combinations such as strength metrics containing duration or cardio metrics containing reps/weight.
