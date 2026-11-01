# Business Rules and Calculation Semantics

## Completed Data

- Volume includes only completed sets with both non-null weight and reps.
- One-rep max and progression include only completed sets with non-null weight and reps.
- A missing one-rep-max history returns `0`, not `404`.

## Formulas and Selection

- Estimated 1RM uses the Epley formula: `weightKg * (1 + reps / 30.0)`.
- Session volume is the sum of `weightKg * reps`.
- Monthly volume groups completed strength-set volume by the user's local calendar month. Completed sets must have non-null weight and reps; months without qualifying sets are omitted.
- Date-range volume sums the same qualifying sets over an inclusive local-date range.
- Progression returns one highest estimated-1RM set per completed session.
- The estimated 1RM and the session volume follow the same rules for the sets they select.

## Batch Behavior

The training totals are aggregated with one query over the completed sessions and sets of the user. The streak is read from the distinct completion timestamps of the user, converted to their local dates, and computed in memory.

The achievements that depend on personal records are not part of this module: they are owned by the [gamification module](../gamification/README.md), which persists them.

## Streaks and Timezones

Completed session timestamps are converted to the user's IANA timezone before dates are deduplicated. The current streak remains alive when the latest active local date is today or yesterday; otherwise it is zero. The longest streak is calculated from consecutive local dates and is retained even when the current streak has expired.

## Ownership and Ranges

Every query is filtered by user ID. Progress uses the half-open interval `[from, to)`. An invalid range currently raises the module's not-found exception rather than a dedicated validation exception.

Monthly volume and muscle intensity use inclusive local-date ranges and convert them to a half-open instant interval using the user's IANA timezone. Muscle intensity returns all catalog muscles, weights impact levels (`PRIMARY=1.0`, `SECONDARY=0.5`, `STABILIZER=0.25`), and normalizes the result to 0–10 relative to the strongest muscle in the selected range.
