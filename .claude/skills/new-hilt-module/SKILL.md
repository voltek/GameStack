---
name: new-hilt-module
description: Create or extend a Hilt module to bind an interface to its implementation or provide a constructed dependency. Use when a new Repository, DAO, Database, API service, or interceptor needs to enter the DI graph.
---

## When to invoke
Whenever something new must be injectable and Hilt cannot figure it out on its own:
- An interface → implementation binding (Repository, DataSource).
- A type this project does not construct itself (Retrofit, OkHttp, Room's
  `RoomDatabase`, a DAO obtained from the Database).
- Anything needing a qualifier because two instances of the same type exist.

**Not needed** when a class is constructor-injectable and depended on by its own
concrete type — `@Inject constructor` alone is enough (e.g. `AuthInterceptor`,
UseCases, ViewModels with `@HiltViewModel`). Adding a redundant `@Provides` for
these widens the graph for nothing.

`project-scaffold` deliberately creates no modules up front; this skill owns
their creation, at the moment a real binding is needed.

## Choosing the module
Name modules after **what they represent**, never a catch-all "CommonModule" or
"AppModule" (CLAUDE.md, Code Conventions → Tier 3). Current modules:

| Module | Owns |
|---|---|
| `NetworkModule` | OkHttp clients, Retrofit instances, API services, the AuthRepository binding |
| `DatabaseModule` | The Room Database, DAOs, and their bindings |

Extend an existing module when the new binding belongs to its concern; create a
new one when it clearly doesn't. Hilt's graph is flat, so a dependency provided
in one module is injectable everywhere — a shared type (e.g. `Json`) can get its
own module without any hierarchy concerns.

## Construction
- `@Module` + `@InstallIn(SingletonComponent::class)` for app-lifetime
  dependencies (all current cases). Use a narrower component only when the
  dependency genuinely should not outlive a screen — and say why.
- Prefer `@Binds` (abstract) over `@Provides` for interface→impl bindings:
  it generates less code and cannot accidentally hold state.
- A module mixing both is an `abstract class` with `@Binds` functions and a
  `companion object` holding the `@Provides` ones — this is the existing
  `NetworkModule` shape; follow it.
- `@Singleton` on anything expensive or stateful (OkHttp, Retrofit, Room
  Database, in-memory caches). Room's Database in particular **must** be a
  singleton — multiple instances open competing connections to one file.
- Room Database needs `@ApplicationContext context: Context`; never pass an
  Activity context (leaks the Activity for the app's lifetime).

## Qualifiers
When two instances of the same type coexist, both need a qualifier — an
unqualified one silently becomes the default and the wrong client gets injected.
The live example: the IGDB OkHttp client carries `AuthInterceptor`, the Twitch
auth client must not (fetching the token cannot depend on already having one).
Define qualifiers as `@Qualifier @Retention(AnnotationRetention.BINARY)`
annotations in `core/data/di/NetworkQualifiers.kt` (or the matching file for
another concern), one file per module's worth of qualifiers.

## Visibility
Only expose a value via `@Provides`/`@Binds` if something outside the module
genuinely injects it directly. Otherwise keep it a private helper function —
the `Json` converter factory in `NetworkModule` is the reference case: used
only to build the Retrofit instances in that same file, so it is private rather
than a binding (CLAUDE.md, Code Conventions → Tier 3).

**Exception — anything that must be `@Singleton` stays a binding**, even when
only consumers inside the same module inject it. Scoping is a property of the
DI graph: a private helper function is re-executed at every call site, so
demoting a `@Singleton @Provides` to a private function silently produces one
instance per consumer. `NetworkModule`'s two `OkHttpClient`s and two `Retrofit`s
are exactly this case — nothing outside the module injects them, but each must
be a single shared instance (the OkHttp connection pool and thread pool are the
whole point of reusing a client). Keep them as `@Provides @Singleton`.

The dividing line is whether a duplicate instance would be harmful, not whether
the value escapes the module. `Json` is cheap and stateless, so a second one
costs nothing and it can be private; an `OkHttpClient` is neither.

If two modules both need a helper, extract it to its own named module rather
than duplicating it or widening one module's responsibility.

## Location
`core/data/di/{Name}Module.kt` for data-layer bindings. A binding used by exactly
one feature may live in `feature/{name}/data/di/` — but prefer `core/data/di/`
unless the isolation is obvious.

## Naming convention
`{Concern}Module` — `NetworkModule`, `DatabaseModule`.

## Quality criteria
- No module named for a layer or lifetime instead of a concern
  (`AppModule`, `CommonModule`, `SingletonModule` are all wrong).
- No `@Provides` for a class that is already constructor-injectable.
- Every duplicated type has a qualifier on **all** of its instances.
- Room Database and network clients are `@Singleton`.
- No unscoped binding exposed that nothing outside the module injects — those
  become private helpers. `@Singleton` ones stay bindings (see Visibility).
- Hilt errors surface at compile time: run `./gradlew build` (not just `test`)
  after adding bindings — a missing binding or duplicate provider fails KSP
  codegen, and unit tests alone won't catch it.
