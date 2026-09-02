# CLAUDE.md — spring-security-kerberos-test

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `kerberos-test`.

## Module

Gradle project `:spring-security-kerberos-test`
(`kerberos/kerberos-test/spring-security-kerberos-test.gradle`). Unlike a typical
`*-test` support module, its fixtures live in `src/main/java` (not `src/test/java`) — this
module's whole purpose is to be consumed as a dependency by other modules' tests (e.g.
`kerberos-client`), so its embedded-KDC test infrastructure has to be main, compiled, shipped
code, not just this module's own internal tests. `org.apache.kerby:kerby-simplekdc` and
`org.junit.jupiter:junit-jupiter` are both hard `api` dependencies for the same reason —
consumers need both on their test classpath transitively.

Test scoping: `./gradlew :spring-security-kerberos-test:test`.

## Package map

Two classes, `kerberos/test/`:

- `MiniKdc` — starts/stops a real, embedded Kerberos Key Distribution Center (backed by Apache
  Kerby's `SimpleKdc`) for integration tests, so `kerberos-core`/`kerberos-web`/`kerberos-client`
  tests can perform real SPNEGO handshakes and ticket issuance against an actual KDC rather than
  mocking JGSS/JAAS internals.
- `KerberosSecurityTestcase` — a JUnit base class wrapping `MiniKdc` lifecycle management
  (start/stop, principal/keytab setup) for tests that need Kerberos infrastructure; other
  `kerberos/*` modules' tests extend this rather than managing a `MiniKdc` instance directly.

When writing or debugging a Kerberos-related test elsewhere in `kerberos/*`, check whether it
extends `KerberosSecurityTestcase` — if so, failures may stem from KDC startup/lifecycle issues in
this module rather than the code under test.
