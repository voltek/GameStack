---
paths:
  - "app/src/**/*.kt"
---

# Writing Kotlin in this app

## Layers
- This project always has a domain layer, which Android treats as optional: a
  ViewModel takes a UseCase, never a Repository. Konsist candidate (#163).
- Domain code imports no Retrofit, Room or Android type, so it stays plain Kotlin.
  The one exception is `javax.inject`. Konsist candidate (#163).
- A DTO or Entity never appears in a signature outside `data/` (ADR 0003). Konsist
  candidate (#163).
- Product rules live in UseCases (ADR 0054).

## Suspend functions
A suspend function never builds a `Result` with `runCatching`: it catches
`CancellationException`, so a cancelled coroutine reaches the user as an error. Use
`try`/`catch` and rethrow `CancellationException` first (ADR 0009). In code that does
not suspend, `runCatching` is fine.
