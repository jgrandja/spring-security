# CLAUDE.md — spring-security-kerberos-client

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `kerberos-client`.

## Module

Gradle project `:spring-security-kerberos-client`
(`kerberos/kerberos-client/spring-security-kerberos-client.gradle`). Depends on
`spring-security-kerberos-core` and `spring-security-kerberos-web` as `implementation` (neither
is referenced directly by this module's own main or test source as of this writing — if you're
touching this build file, check whether the `kerberos-web` dependency is still needed before
assuming it's load-bearing); `spring-web` and `org.apache.httpcomponents:httpclient` are hard
`api` dependencies, since `KerberosRestTemplate` is built directly on Apache HttpClient rather
than a pluggable HTTP abstraction. `spring-security-ldap` is `optional`, gating
`KerberosLdapContextSource`.

Test scoping: `./gradlew :spring-security-kerberos-client:test`. Uses
`spring-security-kerberos-test` (a separate module, `kerberos/kerberos-test`) for shared test
support/fixtures.

## Package map

This module is the *outbound* half of Kerberos support — a Spring app acting as a Kerberos
client to authenticate to some other SPNEGO-protected service (the inverse of `kerberos-web`,
which protects this app's own endpoints):

- `client/KerberosRestTemplate` — a `RestTemplate` subclass that performs the SPNEGO handshake
  transparently on outbound HTTP calls (via Apache HttpClient), for calling a
  Kerberos/SPNEGO-secured REST endpoint.
- `client/config/SunJaasKrb5LoginConfig` — a JAAS `Configuration` (`InitializingBean`) that
  builds the login config needed to obtain a Kerberos ticket via Sun/OpenJDK's JAAS Krb5 login
  module — the client-side counterpart to `kerberos-core`'s JAAS-based login handling.
- `client/ldap/KerberosLdapContextSource` — extends `spring-security-ldap`'s
  `DefaultSpringSecurityContextSource` to bind to an LDAP directory using Kerberos/GSSAPI SASL
  instead of simple bind credentials. This is the one place in the `kerberos/*` modules that
  crosses into another protocol module (`ldap`) rather than just HTTP.
