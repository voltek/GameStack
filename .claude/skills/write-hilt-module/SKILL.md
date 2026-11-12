---
name: write-hilt-module
description: Create or extend a Hilt module to bind an interface to its implementation or provide a constructed dependency. Use when a new Repository, DAO, Database, API service, or interceptor needs to enter the DI graph.
---

## When a module is needed
- An interface bound to its implementation, such as a Repository.
- A type this project does not construct itself: OkHttp, Retrofit, the Room
  Database, a DAO obtained from it.
- Two instances of one type, which need qualifiers.

A class with an `@Inject constructor` that is injected as itself (`AuthInterceptor`,
a UseCase, a `@HiltViewModel`) needs no module, and a `@Provides` for it only adds a
second way to build it.

## Which module
A module is named for what it holds, never for a layer or a lifetime (`AppModule`,
`CommonModule`, `SingletonModule`), in `core/data/di/{Concern}Module.kt`:

| Module | Holds |
|---|---|
| `NetworkModule` | OkHttp clients, Retrofit instances, API services, and the binding of `AuthRepository`, the token the clients use |
| `RepositoryModule` | The binding of each Repository in `core/`, whatever sources it reads |
| `DatabaseModule` (PENDING — not yet defined) | The Room Database and its DAOs |

Extend the module whose concern the binding is; create a new one when none fits. A
binding of a type one feature owns goes in a module under that feature's
`data/di/`, so `core/` never imports a feature.

## Construction
- `@Module @InstallIn(SingletonComponent::class)`. A narrower component only for a
  dependency that must not outlive a screen, with the reason in a comment.
- `@Binds` for an interface bound to its implementation, `@Provides` for a type built
  by hand. A module with both is an `abstract class` holding the `@Binds`, with the
  `@Provides` in its `companion object`, as `NetworkModule` does.
- `@Singleton` on what is expensive or holds state: OkHttp, Retrofit, the Room
  Database. Room's guide asks for one Database instance, since each is expensive.
- The Database takes `@ApplicationContext`; an Activity context would be held for the
  app's lifetime.

## Qualifiers
When two instances of a type coexist, every one of them carries a qualifier: an
unqualified one becomes the default and is injected where the other was meant. The
IGDB `OkHttpClient` carries `AuthInterceptor` and the Twitch one must not, since
fetching the token cannot need a token. Qualifiers are
`@Qualifier @Retention(AnnotationRetention.BINARY)` annotations in one file per
module, such as `NetworkQualifiers.kt`.

## Visibility
A value nothing outside the module injects stays a private function, as the `Json`
converter factory in `NetworkModule` does — unless it must be a single instance. A
private function runs again at every call, so an `OkHttpClient` demoted to one would
give each Retrofit its own connection pool; those stay `@Provides @Singleton` even
though only the module uses them (ADR 0004).

## Check
A missing or duplicate binding fails Hilt's code generation, which `./gradlew build`
and `./gradlew test` both run.
