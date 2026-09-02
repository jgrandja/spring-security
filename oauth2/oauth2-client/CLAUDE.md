# CLAUDE.md — spring-security-oauth2-client

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `oauth2-client`.

## Module

Gradle project `:spring-security-oauth2-client`
(`oauth2/oauth2-client/spring-security-oauth2-client.gradle`). Depends on `spring-security-core`,
`spring-security-oauth2-core`, and `spring-security-web` (`api`), plus `com.nimbusds:
oauth2-oidc-sdk` (`api`). `spring-security-oauth2-jose` is `optional` — OIDC ID token
decoding/validation is opt-in, not a hard requirement of this module. `spring-webflux`/
`reactor-core`, `spring-jdbc`/`spring-r2dbc`, and both Jackson generations are also `optional`.
`jakarta.servlet-api` is `provided`.

Test scoping: `./gradlew :spring-security-oauth2-client:test`, or with a filter:
`./gradlew :spring-security-oauth2-client:test --tests "*OAuth2LoginAuthenticationFilter*"`.

## Servlet/reactive pattern used here

This module mostly follows `core`'s interleave-by-name convention (`Reactive`-prefixed/suffixed
sibling classes in the same flat package), **except** in `web/` and `oidc/`, where it follows
`web`-module-style package splitting (unqualified = servlet, `server` subpackage = reactive).
Don't assume one convention applies module-wide — check per package:

- `registration/`, `authentication/`, `userinfo/` — flat, interleaved by name (e.g.
  `ClientRegistrationRepository`/`ReactiveClientRegistrationRepository`,
  `OAuth2LoginAuthenticationProvider`/`OAuth2LoginReactiveAuthenticationManager`,
  `OAuth2UserService`/`ReactiveOAuth2UserService`).
- `endpoint/` — interleaved, but by *implementation-prefix* rather than `Reactive`: servlet clients
  are named `RestClient*TokenResponseClient` (built on
  `AbstractRestClientOAuth2AccessTokenResponseClient`), reactive clients are named
  `WebClientReactive*TokenResponseClient` (built on
  `AbstractWebClientReactiveOAuth2AccessTokenResponseClient`), both implementing
  `OAuth2AccessTokenResponseClient`/`ReactiveOAuth2AccessTokenResponseClient` respectively.
- `web/` — package-split like the `web` module: servlet lives unqualified at the package root
  (`OAuth2AuthorizationRequestRedirectFilter`, `OAuth2LoginAuthenticationFilter`,
  `HttpSessionOAuth2AuthorizationRequestRepository`, `HttpSessionOAuth2AuthorizedClientRepository`,
  `DefaultOAuth2AuthorizedClientManager`), and `web/server/` is the full reactive mirror
  (`OAuth2AuthorizationRequestRedirectWebFilter`, `web/server/authentication/
  OAuth2LoginAuthenticationWebFilter`, `WebSessionOAuth2ServerAuthorizationRequestRepository`,
  `ServerOAuth2AuthorizedClientRepository`, `DefaultReactiveOAuth2AuthorizedClientManager`).
- `oidc/` — also package-split: `oidc/authentication` interleaves by name
  (`OidcAuthorizationCodeAuthenticationProvider`/`OidcAuthorizationCodeReactiveAuthenticationManager`),
  but `oidc/session` (servlet: `OidcSessionRegistry`) vs `oidc/server/session` (reactive:
  `ReactiveOidcSessionRegistry`) and `oidc/web/logout` vs `oidc/web/server/logout` (RP-initiated
  logout success handlers) split by package, nesting reactive under `server` one level deeper than
  `web/server` does at the module's own top level.

Additional narrow integration-glue packages, analogous to the `web` module's `web.servlet`/
`web.reactive`: `web/client/` (blocking `RestClient` integration —
`OAuth2ClientHttpRequestInterceptor`), `web/reactive/function/client/` (`WebClient` integration —
notably **both** `ServletOAuth2AuthorizedClientExchangeFilterFunction` and
`ServerOAuth2AuthorizedClientExchangeFilterFunction` live here together), and parallel
`web/method/annotation/` vs `web/reactive/result/method/annotation/` packages, each providing an
`@RegisteredOAuth2AuthorizedClient` argument resolver (both classes are named
`OAuth2AuthorizedClientArgumentResolver` — disambiguate by full package, not just class name).

## Package map (non-`web`/`oidc`)

- `registration/` — `ClientRegistration`/`ClientRegistrations` (pure data/builder, no split
  needed) and repository implementations (`InMemoryClientRegistrationRepository`,
  `SupplierClientRegistrationRepository`, plus reactive counterparts).
- `authentication/` — OAuth2 login `AuthenticationProvider`s/`ReactiveAuthenticationManager`s and
  the shared tokens they produce (`OAuth2AuthenticationToken`,
  `OAuth2AuthorizationCodeAuthenticationToken`, `OAuth2LoginAuthenticationToken`).
- `endpoint/` — one request class per grant type, all extending
  `AbstractOAuth2AuthorizationGrantRequest` (`OAuth2AuthorizationCodeGrantRequest`,
  `OAuth2ClientCredentialsGrantRequest`, `OAuth2RefreshTokenGrantRequest`, `JwtBearerGrantRequest`,
  `TokenExchangeGrantRequest`), each with matching servlet/reactive token-response clients (see
  above), plus shared request converters (`DefaultOAuth2TokenRequestHeadersConverter`,
  `DefaultOAuth2TokenRequestParametersConverter`,
  `NimbusJwtClientAuthenticationParametersConverter` for `private_key_jwt`/`client_secret_jwt`
  client authentication).
- `userinfo/` — fetching the authenticated user after token exchange: `OAuth2UserService`/
  `DefaultOAuth2UserService`/`DelegatingOAuth2UserService` and reactive counterparts, sharing
  `OAuth2UserRequest`/`OAuth2UserRequestEntityConverter`.
- `http/` — minimal compared to `oauth2-core`'s `http/converter`: just
  `OAuth2ErrorResponseErrorHandler`, a `ResponseErrorHandler` for parsing OAuth2 error bodies from
  token/userinfo endpoint responses.
- `event/` — client-specific application events (authorization success/failure, etc.).
- `jackson`/`jackson2` — dual Jackson 3/Jackson 2 mixin packages, same pattern as other modules
  (`OAuth2ClientJacksonModule`/`OAuth2ClientJackson2Module`, mirrored mixins e.g.
  `ClientRegistrationMixin`, `OAuth2AuthorizedClientMixin`, `OidcIdTokenMixin`).
- `aot/hint/OAuth2ClientRuntimeHints` — note the extra `hint` nesting level versus some other
  modules' flatter `aot/` package.
- `annotation/` — supporting annotations (e.g. backing `@RegisteredOAuth2AuthorizedClient`).

## Testing note

JDBC/R2DBC-backed registration-repository support is exercised in tests (`r2dbc-h2`,
`r2dbc-spi-test`, `spring-jdbc` as test/optional deps) even though this module does not ship a
JDBC/R2DBC `ClientRegistrationRepository` implementation as a main dependency — check
`spring-security-data` or a similar module before assuming persistence support belongs here.
