# CLAUDE.md — spring-security-oauth2-resource-server

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to
`oauth2-resource-server`.

## Module

Gradle project `:spring-security-oauth2-resource-server`
(`oauth2/oauth2-resource-server/spring-security-oauth2-resource-server.gradle`). Base package is
`org.springframework.security.oauth2.server.resource` — note the ordering (`oauth2.server.resource`,
not `oauth2.resourceserver`). Depends on `spring-security-core`, `spring-security-oauth2-core`, and
`spring-security-web` (`api`). `spring-security-oauth2-jose` (JWT support) and
`com.nimbusds:oauth2-oidc-sdk` (opaque-token introspection support) are both `optional` — a
resource server can authenticate bearer tokens via JWT decoding *or* introspection without needing
both dependencies. `spring-webflux`/`reactor-core` are `optional`, gating reactive support.

Test scoping: `./gradlew :spring-security-oauth2-resource-server:test`, or with a filter:
`./gradlew :spring-security-oauth2-resource-server:test --tests "*JwtAuthenticationProvider*"`.

## Servlet/reactive: split differs by package, same as `oauth2-client`

- `authentication/` and `introspection/` interleave servlet/reactive by name in a flat package
  (the `core`/`oauth2-jose` convention): `JwtAuthenticationProvider`/`JwtReactiveAuthenticationManager`,
  `JwtAuthenticationConverter`/`ReactiveJwtAuthenticationConverter`,
  `JwtGrantedAuthoritiesConverter`/`ReactiveJwtGrantedAuthoritiesConverterAdapter`,
  `JwtIssuerAuthenticationManagerResolver`/`JwtIssuerReactiveAuthenticationManagerResolver`,
  `OpaqueTokenAuthenticationProvider`/`OpaqueTokenReactiveAuthenticationManager`,
  `SpringOpaqueTokenIntrospector`/`SpringReactiveOpaqueTokenIntrospector` (there is no
  `NimbusOpaqueTokenIntrospector` — the introspector implementation class is
  `SpringOpaqueTokenIntrospector`, built on `RestClientOpaqueTokenIntrospector`).
- `web/` package-splits instead (the `web`-module convention): shared/servlet classes sit
  unqualified at `web/` and `web/authentication/` (`BearerTokenAuthenticationFilter`,
  `BearerTokenAuthenticationEntryPoint`, `DefaultBearerTokenResolver`), while reactive
  counterparts live in separate sibling packages — `web/server/` (`BearerTokenServerAuthenticationEntryPoint`)
  and `web/access/server/` (`BearerTokenServerAccessDeniedHandler`), as opposed to the servlet
  `web/access/BearerTokenAccessDeniedHandler`. A `web/reactive/` directory exists on disk but is
  **empty** — vestigial, don't add code there without checking why it's unused first.

Don't assume a single convention when adding new authentication vs. web-layer code — follow
whichever pattern the surrounding package already uses.

## Package map

- `authentication/` — `AbstractOAuth2TokenAuthenticationToken`/`BearerTokenAuthenticationToken`/
  `BearerTokenAuthentication` at the base; JWT authentication
  (`JwtAuthenticationToken`, `JwtAuthenticationProvider`, `JwtAuthenticationConverter`,
  `JwtGrantedAuthoritiesConverter` and its `Delegating`/`Expression` variants,
  `JwtBearerTokenAuthenticationConverter`); multi-issuer resolution
  (`JwtIssuerAuthenticationManagerResolver`); opaque-token authentication
  (`OpaqueTokenAuthenticationProvider`); DPoP (`DPoPAuthenticationToken`,
  `DPoPAuthenticationProvider` — consumes `DPoPProofContext`/`DPoPProofJwtDecoderFactory` directly
  from `oauth2-jose`'s `jwt/` package).
- `introspection/` — kept separate from `authentication/` deliberately:
  `OpaqueTokenIntrospector`/`ReactiveOpaqueTokenIntrospector` interfaces,
  `SpringOpaqueTokenIntrospector`/`SpringReactiveOpaqueTokenIntrospector` implementations,
  `OpaqueTokenAuthenticationConverter`, `OAuth2IntrospectionAuthenticatedPrincipal`,
  `OAuth2IntrospectionException`/`BadOpaqueTokenException`.
- `web/` — `BearerTokenResolver`/`DefaultBearerTokenResolver`/`HeaderBearerTokenResolver`,
  `OAuth2ProtectedResourceMetadataFilter`; `web/authentication/` has
  `BearerTokenAuthenticationFilter`/`BearerTokenAuthenticationConverter` plus the servlet-only
  `DPoPAuthenticationConverter`; `web/access/` and its reactive sibling `web/access/server/` hold
  the access-denied handlers.
- Top level — `BearerTokenError`/`BearerTokenErrorCodes`/`BearerTokenErrors`,
  `InvalidBearerTokenException`, `OAuth2ProtectedResourceMetadata`/
  `OAuth2ProtectedResourceMetadataClaimAccessor`/`ClaimNames`. No `configurer`/`context`
  subpackage — everything module-wide sits either at this top level or in `authentication`/
  `introspection`/`web`.

## Notably absent compared to sibling modules

- No `@AuthenticationPrincipal`-style argument-resolver package (no `web.method.annotation`) —
  resource-server code does not provide its own Spring MVC/WebFlux argument resolver for
  injecting a `Jwt`/`BearerTokenAuthentication` principal.
- No `jackson`/`jackson2` packages and no `aot` package, unlike `core`, `web`, `oauth2-client`.
- DPoP support (`authentication.DPoPAuthenticationProvider`/`DPoPAuthenticationToken`,
  `web.DPoPAuthenticationEntryPoint`, `web.authentication.DPoPAuthenticationConverter`) is
  servlet-only — there is currently no reactive DPoP authentication manager or converter.
