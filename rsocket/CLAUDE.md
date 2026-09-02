# CLAUDE.md — spring-security-rsocket

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `rsocket`.

## Module

Gradle project `:spring-security-rsocket` (`spring-security-rsocket.gradle`). Depends on
`spring-security-core` and the raw `io.rsocket:rsocket-core` protocol library (`api`) — this
module secures RSocket directly at the protocol level, not through Spring's `spring-messaging`
RSocket integration, which is only `optional` (used solely by the small `metadata`/route-matching
glue that needs Spring's `RSocketRequester`/route-mapping types). `spring-security-oauth2-resource-server`
is also `optional`, backing bearer-token authentication over RSocket without a hard dependency on
OAuth2. Reactive-only by nature — RSocket itself is a reactive protocol, so there's no
servlet/blocking equivalent anywhere in this module.

Test scoping: `./gradlew :spring-security-rsocket:test`.

## The `PayloadExchange` abstraction — this module's `Filter`/`Message` equivalent

RSocket's protocol unit is a `Payload` (data + metadata) sent over one of several interaction
models (REQUEST_RESPONSE, REQUEST_STREAM, etc.), not an HTTP request or a `spring-messaging`
`Message`. This module wraps that in its own `PayloadExchange` (`api/PayloadExchange`,
`api/PayloadExchangeType`, `core/DefaultPayloadExchange`) and builds a full parallel security
stack around it, deliberately mirroring `web`'s `Filter`/`RequestMatcher`/`AuthorizationManager`
and `messaging`'s `Message`/`MessageMatcher` design (see
[web/CLAUDE.md](../web/CLAUDE.md)/[messaging/CLAUDE.md](../messaging/CLAUDE.md)) rather than
inventing an unrelated model:

- `api/PayloadInterceptor`/`PayloadInterceptorChain` — the `Filter`/`FilterChain` equivalent.
- `util/matcher/PayloadExchangeMatcher` (+ `PayloadExchangeMatchers` factory,
  `RoutePayloadExchangeMatcher` for matching by RSocket route) — the `RequestMatcher`/
  `MessageMatcher` equivalent.
- `authorization/PayloadExchangeMatcherReactiveAuthorizationManager` — implements `core`'s
  `ReactiveAuthorizationManager<PayloadExchange>` directly, the same interface `web.server`
  uses for HTTP, parameterized on `PayloadExchange` instead of `ServerWebExchange`.
- `authorization/AuthorizationPayloadInterceptor` — enforces the above, analogous to `web`'s
  `AuthorizationFilter`/`messaging`'s `AuthorizationChannelInterceptor`.

When adding a new security concern to this module, look at how `web`/`web.server` or `messaging`
already model the equivalent HTTP/message concept and mirror that shape onto `PayloadExchange`
rather than designing something new.

## Package map

- `core/` — the interceptor pipeline plumbing: `PayloadInterceptorRSocket` (wraps a raw
  `RSocket` to run the interceptor chain), `ContextPayloadInterceptorChain`,
  `PayloadSocketAcceptor`/`PayloadSocketAcceptorInterceptor`/`SecuritySocketAcceptorInterceptor`
  (hooks into RSocket's connection-setup/acceptor phase to install the security interceptors).
- `authentication/` — extracting credentials from a `PayloadExchange`:
  `PayloadExchangeAuthenticationConverter` (+ `AuthenticationPayloadExchangeConverter`) is the
  contract; `BasicAuthenticationPayloadExchangeConverter`/`BearerPayloadExchangeConverter` are
  the two supplied implementations (HTTP Basic-style and Bearer-token-style credentials carried
  in RSocket setup/metadata), `AuthenticationPayloadInterceptor` performs the actual
  authentication, `AnonymousPayloadInterceptor` supplies an anonymous `Authentication` when no
  credentials are present (mirroring `web`'s `AnonymousAuthenticationFilter`).
- `metadata/` — encodes/decodes the credential formats above to/from RSocket metadata payloads:
  `UsernamePasswordMetadata` + `BasicAuthenticationEncoder`/`BasicAuthenticationDecoder`,
  `BearerTokenMetadata` + `BearerTokenAuthenticationEncoder`, `SimpleAuthenticationEncoder`. This
  is where the wire format lives — if authentication metadata isn't being read/written as
  expected, check the encoder/decoder pairing here before the converters in `authentication/`.
- `util/matcher/` — see above; also `PayloadExchangeAuthorizationContext`/
  `PayloadExchangeMatcherEntry` supporting types.
- `api/` — the core, mostly-interface contract types shared by the rest of the module.
