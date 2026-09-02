# CLAUDE.md — spring-security-oauth2-core

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `oauth2-core`.

## Module

Gradle project `:spring-security-oauth2-core` (`oauth2/oauth2-core/spring-security-oauth2-core.gradle`).
Depends on `spring-security-core` (`api`) plus `spring-web` (`api`); `spring-webflux` and
`com.nimbusds:oauth2-oidc-sdk` are `optional`. This module holds the shared OAuth2/OIDC
**vocabulary and value types** — no HTTP client, no filters, no configuration — consumed by
`oauth2-client`, `oauth2-resource-server`, `oauth2-jose`, and `oauth2-authorization-server`. It
has no dependency on any of those; changes here ripple outward to all of them, so treat types in
this module as a stable shared contract, not something to freely reshape for one consumer's needs.

Test scoping: `./gradlew :spring-security-oauth2-core:test`, or with a filter:
`./gradlew :spring-security-oauth2-core:test --tests "*OAuth2AccessToken*"`.

## Package map

- Top-level `oauth2/core/` — the core OAuth2 vocabulary: `OAuth2Token`/`AbstractOAuth2Token`,
  `OAuth2AccessToken`, `OAuth2RefreshToken`, `OAuth2DeviceCode`, `OAuth2UserCode`,
  `AuthorizationGrantType`, `ClientAuthenticationMethod`, `OAuth2Error`/`OAuth2ErrorCodes`,
  `OAuth2AuthenticationException`/`OAuth2AuthorizationException`, `OAuth2TokenValidator`/
  `DelegatingOAuth2TokenValidator`, `ClaimAccessor`, `OAuth2AuthenticatedPrincipal` (extends
  `core`-module's `AuthenticatedPrincipal`). These are framework-agnostic — no servlet/reactive
  split at all in this module, since it's pure data/contract, not runtime behavior.
- `oidc/` — the OpenID Connect layer built on top of the OAuth2 vocabulary:
  `OidcIdToken`/`IdTokenClaimNames`/`IdTokenClaimAccessor`, `OidcUserInfo`/`StandardClaimNames`/
  `StandardClaimAccessor`, `OidcScopes`. Subpackages `oidc/endpoint` (`OidcParameterNames`) and
  `oidc/user` (`OidcUser`/`DefaultOidcUser`/`OidcUserAuthority`, extending `user/OAuth2User`
  types below). If you add a claim or token type, decide whether it's generic OAuth2 (top-level)
  or OIDC-specific (`oidc/`) — don't add OIDC claim names to the top-level package.
- `endpoint/` — request/response model types for the OAuth2 endpoints (authorization, token,
  device authorization): `OAuth2AuthorizationRequest`/`OAuth2AuthorizationResponse`,
  `OAuth2AuthorizationExchange`, `OAuth2AccessTokenResponse`, `OAuth2DeviceAuthorizationResponse`,
  plus parameter name constants (`OAuth2ParameterNames`, `PkceParameterNames`,
  `OidcParameterNames` in `oidc/endpoint`) and converters between the wire map format and typed
  responses (`DefaultMapOAuth2AccessTokenResponseConverter` and its inverse
  `DefaultOAuth2AccessTokenResponseMapConverter`).
- `http/converter/` — Spring `HttpMessageConverter` implementations for the standard OAuth2 wire
  formats (`OAuth2AccessTokenResponseHttpMessageConverter`,
  `OAuth2DeviceAuthorizationResponseHttpMessageConverter`, `OAuth2ErrorHttpMessageConverter`),
  shared by both client-side (`oauth2-client`) and server-side (`oauth2-authorization-server`)
  HTTP handling so the two don't diverge on how these responses are parsed/written.
- `web/reactive/function/` — `OAuth2BodyExtractors`/`OAuth2AccessTokenResponseBodyExtractor` for
  reading these same responses from a reactive `WebClient`; this is the one place in the module
  with reactive-specific code, kept separate from the blocking `http/converter` package since it
  depends on `spring-webflux` (optional).
- `converter/` — generic claim-value coercion used when parsing OIDC/OAuth2 claims from JSON
  (`ClaimTypeConverter`, `ClaimConversionService`, `ObjectToInstantConverter`,
  `ObjectToURLConverter`, etc.), not tied to any single token/claim type.
- `user/` — `OAuth2User`/`DefaultOAuth2User`/`OAuth2UserAuthority`, the generic (non-OIDC)
  authenticated-user model used by `oauth2-client`'s OAuth2 login support; `oidc/user` extends
  these for the OIDC case.
- `authorization/` — `OAuth2AuthorizationManagerFactory`/`DefaultOAuth2AuthorizationManagerFactory`
  and `OAuth2AuthorizationManagers`/`OAuth2ReactiveAuthorizationManagers`, building on the
  `AuthorizationManagerFactory` abstraction from `spring-security-core`'s `authorization` package
  for OAuth2-scope-aware authorization decisions.

## Nimbus usage is narrowly scoped

`com.nimbusds:oauth2-oidc-sdk` is `optional` and, despite being a core dependency of the broader
OAuth2 support, is referenced from only one class in this module —
`web/reactive/function/OAuth2AccessTokenResponseBodyExtractor`. Keep Nimbus usage similarly
contained if extending this module; don't let it leak into the framework-agnostic vocabulary
types, which other modules depend on without necessarily pulling in Nimbus.
