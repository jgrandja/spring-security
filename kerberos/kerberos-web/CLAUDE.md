# CLAUDE.md — spring-security-kerberos-web

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `kerberos-web`.

## Module

Gradle project `:spring-security-kerberos-web`
(`kerberos/kerberos-web/spring-security-kerberos-web.gradle`). Depends on `spring-security-web`
and `jakarta.servlet-api` (`api`); depends on `spring-security-kerberos-core` as
`implementation`, not `api` — consumers of this module do not automatically get
`kerberos-core`'s `AuthenticationProvider`/token types on their own compile classpath, so wiring a
full SPNEGO setup (configuring `KerberosServiceAuthenticationProvider`) requires an explicit
dependency on `kerberos-core` as well. Servlet-only, no reactive counterpart.

Test scoping: `./gradlew :spring-security-kerberos-web:test`.

## Package map

Three classes total, all in `kerberos/web/authentication/` — this module is purely the
servlet-layer HTTP integration (negotiate/challenge-response handling) for the authentication
logic that actually lives in `kerberos-core`:

- `SpnegoEntryPoint` (`AuthenticationEntryPoint`) — sends the `WWW-Authenticate: Negotiate`
  challenge that starts the SPNEGO handshake.
- `SpnegoAuthenticationProcessingFilter` (`OncePerRequestFilter`) — extracts the SPNEGO ticket
  from the `Authorization: Negotiate <token>` request header, wraps it in a
  `KerberosServiceRequestToken` (from `kerberos-core`), and delegates to the configured
  `AuthenticationManager`/`KerberosServiceAuthenticationProvider` for validation.
- `ResponseHeaderSettingKerberosAuthenticationSuccessHandler` (`AuthenticationSuccessHandler`) —
  sets the negotiated response header (the server's own SPNEGO token, if any) on a successful
  handshake.

There's no configurer/DSL class here — wiring `SpnegoEntryPoint` and
`SpnegoAuthenticationProcessingFilter` into a `SecurityFilterChain` is done directly (via
`HttpSecurity`), not through a dedicated Kerberos configurer in the `config` module. Check
`config`'s test suite or samples before assuming a `KerberosConfigurer`-style DSL class exists —
it doesn't.
