# CLAUDE.md — spring-security-cas

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `cas`.

## Module

Gradle project `:spring-security-cas` (`spring-security-cas.gradle`). Depends on
`spring-security-core` and `spring-security-web` (`api`), plus a hard `api` dependency on
`org.apereo.cas.client:cas-client-core` — the actual CAS protocol implementation (ticket
validation against the CAS server) is delegated to that library, not reimplemented here. This
module is a thin Spring Security integration layer on top of it: `AuthenticationProvider`s and
filters that adapt CAS concepts (service tickets, `Assertion`s) into Spring Security's
`Authentication`/`AuthenticationProvider` model. Both Jackson generations
(`com.fasterxml.jackson.core`/`tools.jackson.core`) are `optional`; `jakarta.servlet-api` is
`provided`. Servlet-only — no reactive support, since CAS's client library itself has none.

Test scoping: `./gradlew :spring-security-cas:test`, or with a filter:
`./gradlew :spring-security-cas:test --tests "*CasAuthenticationProvider*"`.

## Package map

- Top level — `ServiceProperties` (the CAS service URL, artifact parameter names, etc.) and
  `SamlServiceProperties extends ServiceProperties` (SAML 1.1 ticket validation variant of the
  CAS protocol — not related to this repo's `saml2` module, which implements SAML 2.0 as a
  service provider; this is CAS's own SAML-flavored ticket format).
- `authentication/` — `CasAuthenticationProvider` (validates a service ticket via a CAS-client
  `TicketValidator`, producing a `CasAuthenticationToken` from the resulting `Assertion`),
  `CasAuthenticationToken`/`CasServiceTicketAuthenticationToken`/`CasAssertionAuthenticationToken`,
  `StatelessTicketCache`/`SpringCacheBasedTicketCache`/`NullStatelessTicketCache` (ticket caching
  for stateless, e.g. RESTful, CAS clients), `ServiceAuthenticationDetails`.
- `web/` — `CasAuthenticationFilter` (processes the service-ticket callback from the CAS server),
  `CasAuthenticationEntryPoint` (redirects unauthenticated requests to the CAS login page),
  `CasGatewayAuthenticationRedirectFilter`/`CasGatewayResolverRequestMatcher` (CAS "gateway"
  mode — attempt silent authentication without prompting for credentials).
- `userdetails/` — `AbstractCasAssertionUserDetailsService` and its
  `GrantedAuthorityFromAssertionAttributesUserDetailsService` implementation, which builds a
  `UserDetails`/authorities directly from CAS assertion attributes rather than a separate
  `UserDetailsService` lookup.
- `jackson`/`jackson2` — dual Jackson mixin packages for this module's `Authentication` token
  types, following the same pattern as `core`/`web`.

## Working in this module

CAS protocol details (ticket formats, validation flow, gateway mode) are defined by
`cas-client-core`, not this module — when behavior seems to originate from the wire protocol
rather than Spring Security's own logic, check that library first rather than assuming a bug here.
