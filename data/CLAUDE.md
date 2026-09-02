# CLAUDE.md — spring-security-data

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `data`.

## Module

Gradle project `:spring-security-data` (`spring-security-data.gradle`). Depends on
`spring-security-core` and `org.springframework.data:spring-data-commons` (`api`) — this is a
narrow bridge module between Spring Security and Spring Data, not a persistence layer for
Spring Security itself (don't confuse it with credential/registration JDBC support that lives
directly in `acl`, `oauth2-client`, `oauth2-authorization-server`, `webauthn`, etc.). No
`optional` dependencies; this module is small and self-contained.

Test scoping: `./gradlew :spring-security-data:test`.

## Package map

- `repository/query/SecurityEvaluationContextExtension` — the entire functional core of this
  module. Implements Spring Data Commons' `EvaluationContextExtension` SPI, which exposes the
  current `Authentication`/principal (plus `hasRole`/`hasPermission`-style expressions, backed by
  `core`'s `SecurityExpressionRoot`, `RoleHierarchy`, `PermissionEvaluator`,
  `AuthorizationManagerFactory`) inside SpEL expressions used in Spring Data query method
  annotations — e.g. `@Query("... where u.id = ?#{principal.id}")` or a derived query using
  `#{hasRole('ADMIN')}`. Registering this class as a bean is the entire integration point; there
  is no filter, configurer, or DSL class in this module.
- `aot/hint/AuthorizeReturnObjectDataHintsRegistrar` — AOT/native-image hints specifically for
  `@AuthorizeReturnObject`-annotated Spring Data repository return types (see `core`'s
  `authorization`/`aot` packages for the base mechanism this extends into Spring Data's proxying).

## Working in this module

Because this module's only real class evaluates arbitrary SpEL against the current
`Authentication`, treat any change here as security-sensitive in the same way as `core`'s SpEL
support (`access.expression`) — a bug here can leak authorization logic errors directly into
persistence-layer queries.
