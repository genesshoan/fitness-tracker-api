# ADR-0012: Persist Achievements Instead of Computing Them On Demand

- **Status:** Proposed
- **Date:** 2026-10-30

## Context

The statistics module computes personal-record achievements whenever a workout
session or a set is read or written. `StatsService.calculateForSession` and
`StatsService.calculateForSet` compare the current metrics against the user's
historical records and return the result inside the response.

This works for what it was built for: telling the user, in the moment, that they
just beat a record. It does not work as a foundation for gamification.

The core limitation is that an achievement is not a state. There is no record of
what a user has unlocked, no progress toward the next unlock, and no way to
determine what is new. Every read recomputes the comparison from the user's full
training history, which requires ranking every historical set for every affected
exercise through five SQL window functions
(`StatsRepository.findRankedSets`). Reading an old session costs proportional to
the user's entire history for the exercises it touches.

The set of achievable achievements is also closed in code. `AchievementType` is a
fixed enum of five personal-record types and `AchievementCalculator` implements
each comparison explicitly. Supporting achievements based on cumulative activity,
such as the number of completed workouts or the length of a training streak, would
require additional enumeration values, additional branches in the calculator, and
a release for every new achievement.

The module boundaries also reflect the temporary nature of this design. The
statistics module reads the workout module's repositories and domain entities
directly, and the workout module depends on the statistics service to build its
responses. The two modules therefore depend on each other, which is the coupling
that ADR-0009 set out to remove.

## Decision

Introduce a dedicated `gamification` module that persists the achievement
lifecycle, and stop computing achievements on demand.

1. **Three persisted layers.** Achievement definitions describe what exists and
   under which rule it unlocks. User achievements record progress, unlock state,
   unlock timestamp, and whether the user has seen the unlock. Personal records
   record the current best value per user, exercise, and metric.

2. **Hybrid rules.** Threshold rules are rows: a metric, an operator, and a
   threshold. They can be added without a deployment. Personal-record rules are
   evaluated in code, because comparing against a previous best requires
   semantics that a threshold cannot express.

3. **Evaluation happens on events, not on reads.** Completing a session or a set
   triggers the evaluation and writes the resulting state. Reads of the catalog
   and of the feed are static lookups of persisted rows.

4. **Aggregates are read from the source at evaluation time.** The evaluation
   reads the user's training aggregates rather than maintaining denormalized
   counters. This keeps the gamification state consistent with the statistics
   module by construction and avoids counters that drift when sessions are edited
   or deleted.

5. **State is monotonic.** A threshold achievement's progress never decreases and
   an unlocked achievement is never revoked. The achievement happened; deleting
   the session that produced it does not unearn it. The current value of a
   personal record is still refreshed, so the displayed record remains accurate.

6. **Achievements are not revoked by destructive operations.** When a set, a
   session exercise, or a session is deleted or edited, the affected personal
   records are recomputed so the stored values stay accurate, but existing
   unlocks are preserved.

7. **The workout module triggers the evaluation through a port.** The evaluation
   runs inside the transaction that completes the session, so the workout state
   and the achievement state commit together. It does not use an asynchronous
   event, because there is no external side effect to isolate.

8. **The notification is an in-app feed.** An unread flag on the unlock marks
   which achievements the user has not seen yet. No email is sent on unlock.

9. **No retroactive unlocks.** Achievements are granted when an evaluation
   happens. There is no backfill that revisits past sessions to award
   achievements the user would otherwise have earned.

## Consequences

### Positive

- The catalog, the feed, and the progress toward the next achievement become
  available without recomputing anything.
- Achievement types that depend on cumulative activity can be added by inserting
  a row, without touching Java code or deploying.
- The per-read cost of a workout session no longer depends on the size of the
  user's training history.
- The dependency between the workout and statistics modules is removed, which
  advances the module boundaries established in ADR-0009.
- The historical behavior of reporting a new record in the response of the set
  that produced it is preserved.

### Negative

- The system now stores state that must be kept consistent with the training
  history, including when sets and sessions are edited or deleted.
- Personal-record comparisons move from a stateless calculation to a stateful one
  and therefore require locking to remain correct under concurrent writes.
- The initial catalog becomes data in the database rather than code, so changes
  to it follow a migration instead of a deployment.
- The unlock semantics are irreversible by design, which is a deliberate trade-off
  against the complexity of revocation.

## Future Considerations

If the achievement rules grow beyond thresholds and personal records, a
general-purpose rule engine may be worth evaluating. The current design keeps the
rule vocabulary narrow on purpose.

Adding points, levels, or a leaderboard does not change the unlock model. Points
require a value on the unlock and a level definition; a leaderboard requires a
ranking scope and a privacy policy. Neither is included in this decision.

If the volume of training history makes reading the aggregates at evaluation time
too expensive, the alternative is to materialize the aggregates and accept the
consistency cost that the current design avoids deliberately.

The achievement evaluation is currently triggered by the workout module. If other
sources of training activity are added, the evaluation entry point should be
generalized rather than duplicated.
