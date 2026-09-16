# ADR-0007: Use Transactional Event Listeners for Post-Commit Side Effects

* **Status:** Accepted
* **Date:** 2026-09-16

## Context

Some application operations modify persistent state and also trigger external side effects, such as sending verification emails or publishing asynchronous messages.

Executing these side effects directly inside the transactional service creates a consistency problem: the external operation may succeed even if the database transaction is later rolled back.

For example, during user registration, the application may create a user and send an email. If the email is sent before the transaction commits and the transaction subsequently fails, the user could receive an email for an account that was never successfully persisted.

Spring provides application events and `@TransactionalEventListener` to associate event handling with a transaction lifecycle.

## Decision

Use Spring application events combined with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for side effects that must only occur after the associated database transaction has successfully committed.

The transactional service publishes an application event after performing the required database changes:

```java
applicationEventPublisher.publishEvent(
    new UserRegisteredEvent(user.getId(), user.getEmail())
);
```

The event listener handles the event only after the transaction commits:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(UserRegisteredEvent event) {
    // side effect
}
```

This establishes the following flow:

```text
@Transactional Service
        │
        ├── Database changes
        │
        └── publishEvent()
                │
                ▼
        Transaction Commit
                │
                ▼
    @TransactionalEventListener
          AFTER_COMMIT
                │
                ▼
        External side effect
```

The event listener must not perform the side effect before the transaction commits.

For asynchronous and durable processing, this mechanism may be combined with RabbitMQ. In that architecture, the `AFTER_COMMIT` listener is responsible for publishing the message to RabbitMQ, while the RabbitMQ consumer is responsible for processing it.

## Alternatives Considered

### Direct execution inside the transactional service

The service could execute the external operation directly after modifying the database.

This was rejected because the external operation may occur even when the database transaction eventually rolls back.

### Standard `@EventListener`

A regular Spring `@EventListener` could handle the application event immediately.

This was rejected for operations that depend on successful transaction completion because the listener is not tied to the transaction commit lifecycle.

### `@Async`

`@Async` could execute the operation asynchronously.

It does not solve the transaction consistency problem by itself and is not durable: pending work exists only in the application process and may be lost if the application terminates before processing it.

`@Async` may still be useful for lightweight in-process asynchronous work, but it is not the mechanism used to guarantee post-commit execution.

### Transaction synchronization implemented manually

The application could register custom transaction synchronization callbacks.

This was rejected because `@TransactionalEventListener` provides the required lifecycle semantics through Spring's standard event infrastructure with less application-specific code.

## Consequences

### Positive

* Prevents external side effects from being triggered by transactions that later roll back.
* Keeps transactional business logic separated from side-effect handling.
* Provides a clear boundary between database operations and external integrations.
* Uses Spring's standard application event infrastructure.
* Allows the event handling mechanism to evolve independently from the transactional service.
* Works naturally with RabbitMQ when durable asynchronous processing is required.

### Negative

* Events are still in-memory until they are handed off to another durable mechanism.
* If the application crashes after the transaction commits but before the event listener executes, the event can be lost.
* Event-driven flows can make execution paths less obvious compared with direct method calls.
* Additional care is required when handling failures in event listeners.

## Reliability Considerations

`AFTER_COMMIT` guarantees that the listener runs only after a successful transaction commit, but it does **not** provide durable event delivery.

For operations where losing the event is unacceptable, the event should be used as the trigger to publish work to a durable messaging system such as RabbitMQ.

The resulting architecture is:

```text
Database Transaction
        │
        ▼
    COMMIT
        │
        ▼
AFTER_COMMIT Listener
        │
        ▼
    RabbitMQ
        │
        ▼
    Consumer
        │
        ▼
 External Operation
`
```
