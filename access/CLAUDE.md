# CLAUDE.md — spring-security-access

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `access`.

## Module

Gradle project `:spring-security-access` (`spring-security-access.gradle`). Depends on
`spring-security-core` and `spring-security-crypto` (`api`); `spring-security-acl`,
`spring-security-messaging`, and `spring-security-web` are `optional`. Confusingly, this module
shares its base package name (`org.springframework.security.access`) with a *different* package
already inside `spring-security-core` (`core/access` — see [core/CLAUDE.md](../core/CLAUDE.md)).
They are not duplicates and do not overlap in content:

- `core`'s `access` package owns the **`@PreAuthorize`/`@PostAuthorize`/`@Secured` annotations
  themselves**, `PermissionEvaluator`, `RoleHierarchy`, and SpEL support for expressions —
  annotation and expression *definitions*.
- This module owns the **legacy voting-based enforcement machinery** that historically processed
  those annotations before Spring Security introduced the `AuthorizationManager` API (now the
  default, living in `core`'s `authorization` package): `AccessDecisionManager`/
  `AccessDecisionVoter`, `SecurityMetadataSource`, and the method-security interceptor stack
  built on them. This is what backs the legacy `@EnableGlobalMethodSecurity` (deprecated in favor
  of `@EnableMethodSecurity`, which uses `AuthorizationManager` directly and does not depend on
  this module).

When working on method security, be clear about which stack you're in: `@EnableMethodSecurity`
(current, `AuthorizationManager`-based, implemented in `core`/`config`) vs.
`@EnableGlobalMethodSecurity` (legacy, voting-based, implemented here). Don't assume a fix in one
stack applies to the other.

Test scoping: `./gradlew :spring-security-access:test`, or with a filter:
`./gradlew :spring-security-access:test --tests "*AffirmativeBased*"`.

## Package map

- Top level — the voting contract: `AccessDecisionManager`/`AccessDecisionVoter`,
  `SecurityMetadataSource`, `ConfigAttribute`/`SecurityConfig`, `AfterInvocationProvider`.
- `vote/` — `AccessDecisionManager` implementations combining voter results:
  `AffirmativeBased`, `ConsensusBased`, `UnanimousBased` (all extending
  `AbstractAccessDecisionManager`); voters: `RoleVoter`, `RoleHierarchyVoter`, `AuthenticatedVoter`,
  `AbstractAclVoter` (for ACL-based voting, active only with the optional `acl` module present).
- `intercept/` — `AbstractSecurityInterceptor`, the base for method/other interceptors enforcing
  an `AccessDecisionManager` decision; `RunAsManager`/`RunAsManagerImpl`/`RunAsUserToken`
  (temporary authority elevation during an invocation, distinct from `web.authentication.switchuser`
  "run as" impersonation in the `web` module — this is a single-invocation elevation, not a
  session-wide identity switch); `AfterInvocationProviderManager`/`AfterInvocationManager`
  (post-invocation filtering/decisions, e.g. filtering a returned collection).
- `prepost/` — the **advice implementations** that evaluate `@PreAuthorize`/`@PostAuthorize`
  attributes (whose annotation types live in `core.access.prepost`):
  `PreInvocationAuthorizationAdvice`/`PostInvocationAuthorizationAdvice`,
  `PreInvocationAuthorizationAdviceVoter` (adapts the advice into an `AccessDecisionVoter`),
  `PrePostAnnotationSecurityMetadataSource`, `PrePostAdviceReactiveMethodInterceptor` (the one
  reactive-aware class in this otherwise non-reactive-split module).
- `expression/method/` — SpEL evaluation of `@PreAuthorize`/`@PostAuthorize` expressions:
  `ExpressionBasedPreInvocationAdvice`/`ExpressionBasedPostInvocationAdvice`,
  `PreInvocationExpressionAttribute`/`PostInvocationExpressionAttribute`.
- `annotation/` — `@Secured` and JSR-250 (`@RolesAllowed` etc.) support:
  `SecuredAnnotationSecurityMetadataSource`, `Jsr250Voter`, `Jsr250MethodSecurityMetadataSource`,
  `Jsr250SecurityConfig`.
- `method/` — `MethodSecurityMetadataSource` and its implementations
  (`AbstractFallbackMethodSecurityMetadataSource`, `DelegatingMethodSecurityMetadataSource`,
  `MapBasedMethodSecurityMetadataSource`) that resolve which `ConfigAttribute`s apply to a given
  method invocation.
- `event/` — authorization decision events (`AuthorizedEvent`, `AuthorizationFailureEvent`,
  `PublicInvocationEvent`, `AuthenticationCredentialsNotFoundEvent`) and `LoggerListener`.
