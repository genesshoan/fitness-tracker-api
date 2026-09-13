# Business Rules and Calculation Semantics

## Completed Data

- Volume includes only completed sets with both non-null weight and reps.
- One-rep max and progression include only completed sets with non-null weight and reps.
- Achievement history is restricted to sessions started before the current session.
- A missing one-rep-max history returns `0`, not `404`.

## Formulas and Selection

- Estimated 1RM uses the Epley formula: `weightKg * (1 + reps / 30.0)`.
- Session volume is the sum of `weightKg * reps`.
- Progression returns one highest estimated-1RM set per completed session.
- Achievement comparison can emit multiple records for one set: max weight, estimated 1RM, more reps at the same weight, max distance, and max duration.
- Ties do not create new achievements because comparisons require a strictly greater value.

## Batch Behavior

`StatsService.calculateForSession` collects all completed sets, extracts distinct exercise IDs, and loads historical record candidates with one `findRankedSets` query for the whole exercise set. Records are then kept in memory and updated after each qualifying set, so a later set in the same session can beat an earlier set from that session.

The repository query ranks weight, 1RM, distance, duration, and reps-at-weight with SQL window functions. It returns only rows that are top-ranked for at least one metric.

## Streaks and Timezones

Completed session timestamps are converted to the user's IANA timezone before dates are deduplicated. The current streak remains alive when the latest active local date is today or yesterday; otherwise it is zero. The longest streak is calculated from consecutive local dates and is retained even when the current streak has expired.

## Ownership and Ranges

Every query is filtered by user ID. Progress uses the half-open interval `[from, to)`. An invalid range currently raises the module's not-found exception rather than a dedicated validation exception.
