# CLAUDE.md — spring-security-messaging

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `messaging`.

## Module

Gradle project `:spring-security-messaging` (`spring-security-messaging.gradle`). Depends on
`spring-security-core` and `spring-messaging` (`api`) — this secures Spring's messaging
abstraction (`Message<T>`/`MessageChannel`), most commonly used for STOMP-over-WebSocket
messaging in a Spring app, but the abstraction itself is transport-agnostic. `spring-security-web`,
`spring-websocket`, `reactor-core`, and `jakarta.servlet-api` are all `optional` — core message
security concepts here don't require a servlet container or WebSocket specifically.

Test scoping: `./gradlew :spring-security-messaging:test`, or with a filter:
`./gradlew :spring-security-messaging:test --tests "*MessageMatcherDelegatingAuthorizationManager*"`.

## Two `AuthenticationPrincipalArgumentResolver` classes — same name, different package, different stack

`messaging.context.AuthenticationPrincipalArgumentResolver` (blocking — resolves
`@AuthenticationPrincipal` on `@MessageMapping` handler methods, e.g. STOMP controllers) and
`messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver` (reactive —
resolves it for reactive messaging handlers, e.g. RSocket) are two unrelated classes with the
identical simple name. Always reference the fully-qualified name or import when discussing either
one — "the `AuthenticationPrincipalArgumentResolver`" alone is ambiguous in this module. The
reactive package also has `handler.invocation.reactive.CurrentSecurityContextArgumentResolver`
with no non-reactive counterpart in `context/`.

## Package map

- `context/` — `SecurityContextChannelInterceptor` (propagates the current `SecurityContext`
  onto an outbound `Message`'s headers, and restores it when a message is received — the
  message-channel equivalent of `web.context`'s HTTP-request-scoped context persistence),
  `SecurityContextPropagationChannelInterceptor`, and the blocking
  `AuthenticationPrincipalArgumentResolver` (see above).
- `handler/invocation/reactive/` — reactive argument resolvers for reactive messaging handler
  methods: `AuthenticationPrincipalArgumentResolver`, `CurrentSecurityContextArgumentResolver`.
- `access/expression/` — SpEL support for message-based authorization:
  `MessageSecurityExpressionRoot` (the root object exposing expressions like `hasRole(...)` in a
  message-security context, analogous to `web`'s HTTP SpEL root), `DefaultMessageSecurityExpressionHandler`,
  `MessageExpressionAuthorizationManager`, `MessageAuthorizationContextSecurityExpressionHandler`.
- `access/intercept/` — `AuthorizationChannelInterceptor` (a `ChannelInterceptor` enforcing
  authorization on inbound messages) and `MessageMatcherDelegatingAuthorizationManager` (the
  message-channel analog of `web`'s `RequestMatcherDelegatingAuthorizationManager` — matches a
  `Message` against a list of `MessageMatcher`s and delegates to the first match's
  `AuthorizationManager`), `MessageAuthorizationContext`.
- `util/` — `MessageMatcher` and implementations: `SimpMessageTypeMatcher` (matches by STOMP
  message type — CONNECT/SUBSCRIBE/MESSAGE/etc.), `PathPatternMessageMatcher` (matches by
  destination path pattern, the message-channel analog of `web.servlet.util.matcher`'s
  `PathPatternRequestMatcher`), `AndMessageMatcher`/`OrMessageMatcher`/
  `AbstractMessageMatcherComposite`.
- `web/` — CSRF support specifically for the STOMP-over-WebSocket handshake:
  `CsrfChannelInterceptor`, `XorCsrfChannelInterceptor`/`XorCsrfTokenUtils` (validates the XOR'd
  CSRF token format used by `web.csrf.XorCsrfTokenRequestAttributeHandler`, applied to the
  CONNECT frame rather than an HTTP request).

## Working in this module

When adding a new cross-cutting message-security concern, check whether an HTTP (`web`) analog
already exists first — most of this module's abstractions (`MessageMatcher`,
`MessageSecurityExpressionRoot`, `MessageMatcherDelegatingAuthorizationManager`,
`SecurityContextChannelInterceptor`) are deliberate message-channel mirrors of a `RequestMatcher`/
SpEL/`AuthorizationManager`/context-persistence concept in `web`, and new functionality here
should generally follow the same shape as its HTTP counterpart rather than inventing a new one.
