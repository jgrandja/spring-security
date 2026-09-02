# CLAUDE.md — spring-security-web

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `web`.

## Module

Gradle project `:spring-security-web` (`spring-security-web.gradle`). Depends on
`spring-security-core` (`api`); no direct dependency on `spring-security-crypto` (pulled in
transitively via `core`). `jakarta.servlet:jakarta.servlet-api` is `provided` (compile-time only,
not bundled) so reactive-only applications aren't forced to carry the servlet API.
`spring-webmvc` and `spring-webflux` are both `optional`, reflecting that this single module
supports both stacks. Also has a custom `javascript`/`syncJavascript` build task that pulls the
bundled WebAuthn client JS from `:spring-security-javascript` into resources at build time (see
[javascript/CLAUDE.md](../javascript/CLAUDE.md)) — don't hand-edit synced JS under `build/`.

Test scoping: `./gradlew :spring-security-web:test`, or with a filter:
`./gradlew :spring-security-web:test --tests "*FilterChainProxy*"`.

## Servlet vs. reactive: three different strategies, not one

Unlike `config` (clean `servlet`/`reactive` sibling packages) or `core` (interleaved
`Reactive`-prefixed classes in the same package), `web` mixes approaches:

- **Default/unqualified = servlet.** Most of the module (`authentication`, `csrf`, `session`,
  `access`, `savedrequest`, `context`, `firewall`, `header`, ...) is servlet code with no `servlet`
  qualifier — it's simply the implicit default.
- **`server/` = the reactive mirror of the whole module.** `web.server` re-implements the same
  concerns for WebFlux: `WebFilterChainProxy`/`SecurityWebFilterChain` (reactive counterparts of
  `FilterChainProxy`/`SecurityFilterChain`), `ServerAuthenticationEntryPoint`, and its own
  `authentication`, `authorization`, `context`, `csrf`, `firewall`, `header`, `savedrequest`, `ui`
  subpackages. When fixing a bug in a servlet-side filter/handler under top-level `web`, check for
  a `web.server` counterpart with the same bug.
- **`servlet/` and `reactive/` are narrow integration-glue packages, not full mirrors.** `servlet/`
  holds servlet/Spring-MVC-specific helpers (e.g. `servlet/util/matcher` —
  `PathPatternRequestMatcher`, the modern replacement for the removed `AntPathRequestMatcher`,
  which depends on Spring MVC's `PathPattern` support and so can't live in the generic
  `util/matcher`). `reactive/` holds WebFlux MVC integration helpers (`reactive/result/view`,
  `reactive/result/method` — e.g. CSRF view resolvers/argument resolvers). Don't confuse these with
  `server/` — `servlet/`/`reactive/` are framework-integration shims, `server/` is the reactive
  security engine itself.

## Core abstractions

- `SecurityFilterChain` — pairs a `RequestMatcher` with an ordered list of filters.
- `FilterChainProxy` — the servlet `Filter` (wired in via `DelegatingFilterProxy`) that holds an
  ordered list of `SecurityFilterChain`s and delegates each request to the filters of the first
  chain whose matcher matches.
- Reactive equivalents: `SecurityWebFilterChain` / `WebFilterChainProxy` in `server/`.

## `authentication/` — one subpackage per mechanism

Generic filters/entry points/handlers (form login, `AuthenticationFailureHandler`, etc.) sit at the
top level of `authentication/`; each specific mechanism gets its own subpackage: `www` (HTTP
Basic/Digest), `preauth` (pre-authenticated/header/X509-style), `rememberme`, `switchuser` ("run
as" impersonation), `ott` (one-time-token login), `logout`, `session` (session-authentication
strategies invoked on login), `ui` (login page rendering), `password`. Adding a new authentication
mechanism should generally follow this same one-subpackage-per-mechanism pattern rather than adding
more top-level classes.

## Hardening packages

- `access/` + `access/intercept` — authorization enforcement for servlet requests
  (`ExceptionTranslationFilter`, `AccessDeniedHandler`, and in `intercept`: `AuthorizationFilter`,
  `RequestMatcherDelegatingAuthorizationManager`, `RequestAuthorizationContext`). Note there is
  **no top-level `web.authorization` package** on the servlet side — that logic lives under
  `access.intercept`; only the reactive side has a dedicated `web.server.authorization`.
- `csrf/` — `CsrfFilter`, `CsrfToken`/`CsrfTokenRepository` implementations, request-handling
  strategy (`XorCsrfTokenRequestAttributeHandler`), `CsrfAuthenticationStrategy`.
- `session/` — `SessionManagementFilter`, `ConcurrentSessionFilter`,
  `SessionInformationExpiredStrategy`, `InvalidSessionStrategy`, `DisableEncodeUrlFilter`.
- `firewall/` — `HttpFirewall`/`StrictHttpFirewall`, `FirewalledRequest`/`FirewalledResponse`,
  `RequestRejectedException`/Handler — request normalization/validation before it reaches the rest
  of the chain.
- `savedrequest/` — post-login redirect: `RequestCache` (+ session/cookie-backed implementations),
  `SavedRequest`, `RequestCacheAwareFilter`.

## `context/` vs. `core/context`

`web.context` (`SecurityContextRepository` and its `HttpSession`/request-attribute/cookie-backed
implementations, plus `SecurityContextHolderFilter`) is the **web-layer persistence** of the
security context across requests. It is distinct from `core.context` in the `core` module, which
owns `SecurityContextHolder`/`SecurityContextHolderStrategy` — the in-memory storage mechanism
itself. `web.context` reads/writes through whatever `core.context` exposes; it doesn't replace it.

## `util/matcher`

Generic `RequestMatcher` implementations independent of servlet/MVC specifics:
`RegexRequestMatcher`, `AnyRequestMatcher`, `OrRequestMatcher`/`AndRequestMatcher`/
`NegatedRequestMatcher`, `MediaTypeRequestMatcher`, `RequestHeaderRequestMatcher`,
`DispatcherTypeRequestMatcher`, `ParameterRequestMatcher`, `IpAddressMatcher`, `RequestMatchers`
(factory). Path-pattern matching specifically lives in `servlet/util/matcher` instead (see above)
because it depends on Spring MVC.

## Dual Jackson support

Like `core`, this module has parallel `jackson` (Jackson 3 / `tools.jackson`) and `jackson2`
(Jackson 2 / `com.fasterxml.jackson`) packages — both under top-level `web/` and mirrored again
under `web/server/`. A new serializable type generally needs mixins in all four locations if it's
usable from both stacks.
