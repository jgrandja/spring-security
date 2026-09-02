# CLAUDE.md — spring-security-oauth2-authorization-server

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to
`oauth2-authorization-server`.

## Module

Gradle project `:spring-security-oauth2-authorization-server`
(`oauth2/oauth2-authorization-server/spring-security-oauth2-authorization-server.gradle`). Base
package: `org.springframework.security.oauth2.server.authorization`. This module is the former
standalone **Spring Authorization Server** project, absorbed into this repo — a full OAuth2/OIDC
Authorization Server implementation, not just a support library. Depends on
`spring-security-web`, `spring-security-oauth2-core`, `spring-security-oauth2-jose`, and, notably,
**`spring-security-oauth2-resource-server`** (all `api`) — the last is used to reuse JWT
validation/introspection logic rather than reimplementing it. `spring-jdbc` is `optional`, gating
the `Jdbc*` repository/service implementations. Jackson 3 (`tools.jackson.core`) is a hard `api`
dependency here (unlike most modules, where it's optional); Jackson 2 remains `optional` for the
legacy `jackson2` mixins.

Test scoping: `./gradlew :spring-security-oauth2-authorization-server:test`, or with a filter:
`./gradlew :spring-security-oauth2-authorization-server:test --tests "*OAuth2TokenEndpointFilter*"`.

## No reactive support — servlet only

Unlike every other `oauth2-*` module, there is **no `Reactive`-prefixed class and no
`server`/reactive subpackage anywhere** in this module. Spring Authorization Server never
supported WebFlux prior to the merge, and that has carried through unchanged. Don't add a reactive
counterpart here by default the way you might in `oauth2-client`/`oauth2-resource-server` — confirm
there's actual demand/precedent first, since this is a deliberate scope boundary, not an oversight.

## The Java config DSL lives outside this module

There is no `config` package under this module's base package. `OAuth2AuthorizationServerConfigurer`
and the entire Java DSL configurer tree live in the separate top-level `config` module, under
`config/annotation/web/configurers/oauth2/server/authorization/`. When changing authorization-server
behavior that's exposed through the DSL, expect to touch both this module and `config`.

## Package map

- Top level (domain model, not a subpackage) — `OAuth2Authorization`(+`Builder`,`Token<T>`),
  `OAuth2AuthorizationCode`, `OAuth2AuthorizationConsent`, `OAuth2TokenType`,
  `OAuth2TokenIntrospection`; services `OAuth2AuthorizationService`/`OAuth2AuthorizationConsentService`
  with `InMemory*`/`Jdbc*` implementations; server/client metadata
  (`OAuth2AuthorizationServerMetadata`, `OAuth2ClientRegistration`).
- `client/` — `RegisteredClient` and `RegisteredClientRepository` (+ `InMemory*`/`Jdbc*`
  implementations) — the registered-OAuth2-client model, distinct from `oauth2-client`'s
  `ClientRegistration` (that's the *relying-party's* view of an external provider; this is the
  *authorization server's* view of a client it serves).
- `authentication/` — the largest package, flat (no per-grant subpackages), organized by
  mechanism via naming convention rather than package structure:
  - Authorization code / PAR / consent / PKCE: `OAuth2AuthorizationCodeRequestAuthenticationProvider`,
    `OAuth2AuthorizationCodeAuthenticationProvider`, `OAuth2AuthorizationConsentAuthenticationProvider`,
    `OAuth2PushedAuthorizationRequestAuthenticationProvider`, `CodeVerifierAuthenticator`.
  - Client authentication: `ClientSecretAuthenticationProvider`, `PublicClientAuthenticationProvider`,
    `JwtClientAssertionAuthenticationProvider`, `X509ClientCertificateAuthenticationProvider`,
    `DPoPProofVerifier` (this module's own DPoP proof verification for token endpoint requests,
    distinct from `oauth2-resource-server`'s resource-side DPoP support).
  - Grant-specific: `OAuth2ClientCredentialsAuthenticationProvider`, `OAuth2RefreshTokenAuthenticationProvider`,
    device code (`OAuth2DeviceAuthorizationRequestAuthenticationProvider`,
    `OAuth2DeviceAuthorizationConsentAuthenticationProvider`, `OAuth2DeviceCodeAuthenticationProvider`,
    `OAuth2DeviceVerificationAuthenticationProvider`), token exchange RFC 8693
    (`OAuth2TokenExchangeAuthenticationProvider`, `OAuth2TokenExchangeActor`).
  - Introspection/revocation: `OAuth2TokenIntrospectionAuthenticationProvider`,
    `OAuth2TokenRevocationAuthenticationProvider`.
  - Dynamic client registration: `OAuth2ClientRegistrationAuthenticationProvider`.
  - Common base: `OAuth2AuthorizationGrantAuthenticationToken`, `OAuth2AuthenticationProviderUtils`.
  - Every mechanism follows a `*Provider`/`*Token`/`*Context` triad (and often `*Validator`) — when
    adding a new grant type or auth mechanism, follow this same triad rather than inventing a new
    shape.
- `web/` — one servlet `Filter` per endpoint: `OAuth2AuthorizationEndpointFilter`,
  `OAuth2PushedAuthorizationRequestEndpointFilter`, `OAuth2TokenEndpointFilter`,
  `OAuth2ClientAuthenticationFilter`, `OAuth2TokenIntrospectionEndpointFilter`,
  `OAuth2TokenRevocationEndpointFilter`, `OAuth2AuthorizationServerMetadataEndpointFilter`,
  `NimbusJwkSetEndpointFilter`, `OAuth2DeviceAuthorizationEndpointFilter`,
  `OAuth2DeviceVerificationEndpointFilter`, `OAuth2ClientRegistrationEndpointFilter`.
  `web/authentication/` mirrors `authentication/` one-to-one with an `AuthenticationConverter` and
  success/failure handler per mechanism (e.g. `JwtClientAssertionAuthenticationConverter`,
  `OAuth2DeviceCodeAuthenticationConverter`) — a new mechanism in `authentication/` almost always
  needs a matching converter here.
- `oidc/` — OIDC support layered the same way as the module overall: top-level domain
  (`OidcClientRegistration`, `OidcProviderConfiguration`), `oidc/authentication` (provider/token/
  context per OIDC concern — client registration, `OidcUserInfoAuthenticationProvider`,
  `OidcLogoutAuthenticationProvider` for RP-Initiated Logout), `oidc/web` (matching endpoint
  filters — `OidcProviderConfigurationEndpointFilter` for issuer discovery,
  `OidcUserInfoEndpointFilter`, `OidcLogoutEndpointFilter`), `oidc/http` (message converters), and
  `oidc/converter` (`RegisteredClient` ↔ `OidcClientRegistration` conversion). `OidcPrompt` is an
  exception and sits in the module's top-level `authentication/` package, not under `oidc/`.
- `settings/` — configuration value objects: `AuthorizationServerSettings`, `ClientSettings`,
  `TokenSettings`, `OAuth2TokenFormat`.
- `context/` — `AuthorizationServerContext`/`AuthorizationServerContextHolder`, used for
  multi-tenancy/issuer resolution (analogous in spirit to `core`'s `SecurityContextHolder`, but a
  separate, request-scoped holder specific to this module).
- `token/` — token generation and claims customization: `OAuth2TokenGenerator`/
  `DelegatingOAuth2TokenGenerator`, `OAuth2AccessTokenGenerator`, `OAuth2RefreshTokenGenerator`,
  `JwtGenerator`, `JwtEncodingContext`, `OAuth2TokenCustomizer` (the extension point for adding
  custom claims to issued tokens).
- `jackson`/`jackson2` — dual Jackson packages as elsewhere, but asymmetric: `jackson2` (legacy)
  has extra mixins (`DurationMixin`, `HashSetMixin`, `UnmodifiableMapMixin`) not present in the
  newer `jackson` package.
- `aot` — the package directory exists but currently has no classes in it.
