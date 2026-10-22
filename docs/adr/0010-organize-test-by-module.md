# ADR-0010: Organize Tests by Module and Isolate External Infrastructure

- **Status:** Accepted
- **Date:** 2026-10-22

## Context

As the application has grown into a modular monolith, the number and variety of tests have increased. Tests cover different concerns, including application logic, persistence, module integration, and external infrastructure.

The previous test directory structure grouped tests primarily by test type, such as unit tests and integration tests. While this structure was manageable when the test suite was smaller, it became increasingly difficult to navigate as more modules and test classes were introduced.

Grouping tests only by type separates tests belonging to the same module across multiple directories. This makes module-specific maintenance more cumbersome and complicates refactoring, particularly when changes affect several related tests.

The architectural refactoring also requires existing tests to be reorganized and updated. Some tests need to be split into smaller classes, dependencies and mocks need to be adjusted, and tests that were previously coupled to internal implementations need to be adapted to the new module boundaries.

Additionally, starting external infrastructure for every test would increase execution time and make the feedback loop slower. This is particularly relevant for integrations such as SMTP, object storage, and other external services.

The test suite must remain useful for validating application behavior without making routine builds unnecessarily expensive.

## Decision

Organize tests primarily by application module, mirroring the relevant structure of the production source code.

Within each module, group tests according to their responsibilities and test type where appropriate. This keeps related tests together while preserving a distinction between unit tests and integration tests.

The testing strategy will follow these principles:

1. **Mirror the production structure.** Test packages and directories should reflect the modules and relevant package structure of the application.
2. **Keep tests close to the functionality they validate.** Tests for a module should be discoverable without navigating across unrelated module directories.
3. **Use mocks for external infrastructure in unit tests.** Dependencies such as mail senders and external service clients should be mocked when the purpose of the test is to validate application or adapter behavior without requiring the real infrastructure.
4. **Test failure handling explicitly.** Unit tests should verify how the application translates or handles failures returned by external dependencies.
5. **Introduce integration tests selectively.** Real infrastructure integrations should be tested separately when validating actual framework configuration, protocol interactions, persistence behavior, or integration contracts provides meaningful additional confidence.
6. **Keep routine test execution practical.** External containers and services should not be required for every unit test. Integration tests may use dedicated infrastructure, including containerized services, where justified.

During the architectural refactoring, existing tests may be reorganized, split, and updated to reflect the new structure and dependencies. The objective is to preserve meaningful behavioral coverage while improving test isolation and maintainability.

## Consequences

### Positive

- Tests are easier to locate and maintain within their respective modules.
- Changes to a module's implementation are less likely to require navigating unrelated test directories.
- The test organization reflects the application's modular architecture.
- Unit tests remain independent of external service availability.
- Routine test execution avoids unnecessary infrastructure startup and associated overhead.
- Integration tests can focus on validating behavior that mocks cannot establish.

### Negative

- Reorganizing the existing suite requires an initial refactoring effort.
- The mirrored directory structure may require updates when production packages or module boundaries change.
- Mock-based tests cannot establish that external integrations work correctly against real infrastructure.
- Integration failures may remain undetected until the relevant integration tests are executed.

## Future Considerations

Introduce dedicated integration tests for external infrastructure where they provide meaningful additional confidence, prioritizing critical integrations and behaviors that cannot be adequately validated with mocks.

Evaluate the execution time of the complete test suite as the application grows. If integration tests become expensive, separate their execution from the fast unit-test feedback loop while retaining appropriate automated coverage in CI.

Test organization and infrastructure strategy may be revised independently if the application's testing requirements change.
