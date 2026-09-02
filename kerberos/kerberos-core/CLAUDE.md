# CLAUDE.md — spring-security-kerberos-core

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `kerberos-core`.

## Module

Gradle project `:spring-security-kerberos-core`
(`kerberos/kerberos-core/spring-security-kerberos-core.gradle`). Only dependency is
`spring-security-core` (`api`) — **no external Kerberos library**. Kerberos/SPNEGO support is
built entirely on the JDK's own `org.ietf.jgss` (JGSS) and `javax.security.auth.kerberos` APIs
plus standard JAAS login (`javax.security.auth.login`), not a third-party client library the way
`cas` wraps `cas-client-core`. Sibling modules in `kerberos/`: `kerberos-client` (outbound
SPNEGO-authenticated HTTP calls, consumes this module), `kerberos-web` (inbound SPNEGO
authentication filter for servlet apps, consumes this module), `kerberos-test` (shared test
support). This module holds the shared authentication model both depend on.

Test scoping: `./gradlew :spring-security-kerberos-core:test`.

## Package map

All main code lives in one package, `kerberos/authentication/` (there's no split top-level
package like other modules) — a deliberately small, flat module:

- **Server/service-side (validating an incoming ticket):** `KerberosServiceAuthenticationProvider`
  (an `AuthenticationProvider` + `InitializingBean` that validates a SPNEGO service ticket via a
  `KerberosTicketValidator`), `KerberosServiceRequestToken` (the `Authentication` carrying the raw
  ticket bytes before validation), `KerberosTicketValidator`/`KerberosTicketValidation` (the
  validation contract and its result, wrapping the resolved `KerberosPrincipal` and GSS context).
  Consumed by `kerberos-web`'s inbound filter.
- **Client-side (obtaining/using a ticket):** `KerberosAuthenticationProvider` (authenticates a
  `KerberosUsernamePasswordAuthenticationToken` — username/password — by performing a JAAS
  Kerberos login and producing a ticket-granting credential), `KerberosClient` (interface for
  actually performing SPNEGO ticket acquisition against a target service),
  `KerberosMultiTier`/`JaasSubjectHolder` (multi-tier/constrained-delegation support: using a
  service's own credentials to obtain a ticket on behalf of an already-authenticated principal, a
  distinct scenario from a simple client login). Consumed by `kerberos-client`.
- `aot/hint/KerberosRuntimeHints` — AOT/native-image hints, same pattern as other modules.

When adding Kerberos functionality, decide up front whether it's server-side (validating tickets
presented to this app) or client-side (this app acquiring tickets to call something else) — the
two authentication providers and their supporting types are not interchangeable, and each has a
different consuming module.
