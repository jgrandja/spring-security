# CLAUDE.md — spring-security-core

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `core`.

## Module

Gradle project `:spring-security-core` (`spring-security-core.gradle`). Foundational module: the
authentication/authorization model that `spring-security-web`, `spring-security-config`, and every
feature module (`oauth2-*`, `saml2-*`, `cas`, `ldap`, ...) build on. Depends only on
`spring-security-crypto` plus core Spring Framework jars — no dependency on `web` or `config`.

Test scoping: `./gradlew :spring-security-core:test`, or with a filter:
`./gradlew :spring-security-core:test --tests "*ProviderManager*"`.

## Package map

- `core/` — root abstractions: `Authentication`, `GrantedAuthority`, `AuthenticationException`,
  plus `core/context` (`SecurityContext`/`SecurityContextHolder`), `core/userdetails`
  (`UserDetails`, `UserDetailsService`), `core/session`, `core/token`, `core/authority`.
- `authentication/` — `AuthenticationManager`/`AuthenticationProvider` implementations
  (`ProviderManager`, `AnonymousAuthenticationProvider`, `RememberMeAuthenticationProvider`, ...)
  and their reactive counterparts (`ReactiveAuthenticationManager`,
  `DelegatingReactiveAuthenticationManager`, `UserDetailsRepositoryReactiveAuthenticationManager`).
  Subpackages: `password` (password encoding/migration), `dao` (`DaoAuthenticationProvider`),
  `ott` (one-time-token auth), `jaas`, `event`.
- `authorization/` — the newer `AuthorizationManager<T>`/`ReactiveAuthorizationManager<T>` API
  (`AuthorityAuthorizationManager`, `AuthenticatedAuthorizationManager`,
  `AuthorizationManagerFactory`, factor-based authorization via `RequiredFactor`/
  `AllRequiredFactorsAuthorizationManager`). Subpackages: `method` (method-security wiring),
  `event`.
- `access/` — the older access-control API this is layered under/alongside: `PreAuthorize`/
  `PostAuthorize`/`PreFilter`/`PostFilter` annotations (`access/prepost`), `@Secured`
  (`access/annotation`), `RoleHierarchy` (`access/hierarchicalroles`), SpEL support
  (`access/expression`), `PermissionEvaluator`/`AccessDeniedException` at the top level. When
  changing authorization behavior, check whether both `access` (annotation/SpEL entry points) and
  `authorization` (the `AuthorizationManager` actually invoked) need updating.
- `provisioning/` — `UserDetailsManager` implementations (in-memory, JDBC-backed elsewhere).
- `util/matcher` — general-purpose matchers shared beyond just web request matching.
- `aot/` — Spring AOT/native-image hint registrars (`CoreSecurityRuntimeHints`,
  `PrePostAuthorizeHintsRegistrar`, `AuthorizeReturnObjectHintsRegistrar`) — must be kept in sync
  when adding reflection-sensitive types under `access`/`authorization`.
- `scheduling`, `task`, `concurrent` — `SecurityContext`-propagating decorators for
  `@Scheduled`/`Executor`/`Callable` usage.

## Dual Jackson support: `jackson` vs `jackson2`

Two parallel, near-identical sets of Jackson mixins/modules exist:
- `jackson2/` — for Jackson 2 (`com.fasterxml.jackson.*`), the long-standing integration.
- `jackson/` — for Jackson 3 (`tools.jackson.*`), a newer parallel implementation.

Both are `optional` dependencies in `spring-security-core.gradle` (`com.fasterxml.jackson.core:
jackson-databind` and `tools.jackson.core:jackson-databind` respectively). Class names largely
mirror each other with the package as the distinguishing signal (e.g.
`jackson/CoreJacksonModule.java` vs `jackson2/CoreJackson2Module.java`). A new serializable
security type (e.g. a new `Authentication` or `GrantedAuthority` implementation) generally needs a
mixin added in **both** packages, not just one.

## Servlet vs. reactive

Unlike `web`/`config` where servlet and reactive code sit in separate sibling packages, in `core`
they're often interleaved in the same package with a `Reactive`-prefixed or suffixed class next to
its blocking counterpart (e.g. `AuthenticationManager` / `ReactiveAuthenticationManager`,
`ProviderManager` / `DelegatingReactiveAuthenticationManager`, `AuthorizationManager<T>` /
`ReactiveAuthorizationManager<T>`). When adding a new authentication or authorization primitive,
check whether a reactive equivalent is expected alongside it.
