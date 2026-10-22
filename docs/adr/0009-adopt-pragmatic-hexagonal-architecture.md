# ADR-0009: Adopt Pragmatic Hexagonal Architecture for Module Boundaries

- **Status:** Accepted
- **Date:** 2026-10-22

## Context

The application is structured as a modular monolith, but some modules have become tightly coupled to the internal implementations of other modules. In particular, modules can directly access repositories owned by other modules, bypassing their intended boundaries and making dependencies between modules less explicit.

This coupling makes it harder to understand module responsibilities, change internal implementations independently, and maintain a clear separation of concerns as the application grows.

A refactoring effort was therefore started to introduce explicit interfaces at module boundaries and reduce direct dependencies on other modules' internal implementations.

A strict implementation of Hexagonal Architecture was considered as a possible approach. However, applying it uniformly would introduce additional ports, adapters, and abstractions throughout the application, including areas where hiding framework-specific implementations provides little practical value.

The primary objective is not to isolate the entire application from Spring or to make every component framework-independent. It is to establish clearer module boundaries and reduce unnecessary coupling while keeping the architecture understandable and maintainable.

The refactoring also needs to remain manageable alongside ongoing development. Reworking the entire application at once would significantly increase the scope and risk of the changes.

## Decision

Adopt a pragmatic, module-oriented application of Hexagonal Architecture within the existing modular monolith.

The architecture will follow these principles:

1. **Explicit module boundaries.** Modules should interact through clearly defined interfaces rather than directly accessing the internal implementations of other modules.
2. **Dependency inversion at meaningful boundaries.** Where a module requires functionality owned by another module, introduce an appropriate interface or contract instead of depending directly on its repository or internal service implementation.
3. **Pragmatic use of ports and adapters.** Introduce abstractions where they help enforce module boundaries or isolate meaningful external dependencies. Do not introduce additional layers or adapters solely to achieve strict architectural purity.
4. **Keep framework integration practical.** Spring and Spring Boot remain first-class implementation technologies. The architecture does not require every framework-specific component to be hidden behind an abstraction.
5. **Incremental migration.** Refactor modules progressively instead of attempting a complete architectural rewrite. Existing cross-module dependencies may remain temporarily, but should be addressed as the relevant modules are refactored.

The authentication and user modules were selected as initial areas for this refactoring. Subsequent changes extend the same principles to other modules as development and maintenance require.

## Consequences

### Positive

- Module dependencies become more explicit and easier to understand.
- Modules become less dependent on the internal persistence implementations of other modules.
- Internal implementations can be changed with fewer cascading modifications across the application.
- The architecture remains compatible with a conventional Spring Boot modular monolith.
- The refactoring can proceed incrementally without requiring a large, high-risk rewrite.
- The number of abstractions can be kept proportional to their practical value.

### Negative

- Some cross-module dependencies remain while the migration is in progress.
- Introducing interfaces and updating existing dependencies requires additional refactoring and test maintenance.
- Module boundaries will not be enforced perfectly until the remaining direct dependencies are addressed.
- The approach requires ongoing discipline to prevent new dependencies on other modules' internal implementations.

## Future Considerations

As additional modules are refactored, evaluate whether remaining cross-module dependencies should be replaced with explicit contracts.

If the application grows sufficiently to justify stronger architectural enforcement, consider adding automated module-boundary checks or adopting stricter rules for module dependencies.

Any additional abstractions should be introduced to solve an identifiable coupling or maintainability problem rather than to satisfy architectural purity alone.
