# Statistics Module

Provides user-scoped workout volume, estimated one-repetition maximum, streak, progression, and achievement data.

![Statistics architecture](stats-architecture.png)

## Documentation

- [API Reference](api.md)
- [Business Rules and Calculation Semantics](business-rules.md)
- [Frontend Integration](frontend.md)

The statistics implementation uses JDBC aggregate queries. Batch behavior and implicit defaults are documented explicitly in the business-rules guide.

![Statistics request flow](stats-sequence.png)
