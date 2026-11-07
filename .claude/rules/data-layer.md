---
paths:
  - "app/src/main/**/data/**"
---

# The data layer
- A DTO never converts straight to an Entity: go through the Domain model, so a change
  to the API cannot reshape the database (ADR 0003). Konsist candidate (#163).
- A Repository holds the rules of its own data: where it comes from and how a write
  changes what is stored, such as stamping `updatedAt` and `completedAt`. A rule needing
  more than old and new stored state goes to a UseCase (ADR 0054).
- `UserGameEntity` (PENDING — no Room code yet): one row per game the user touched.
  List status and rating are independent, so they never split into two tables.
