# CLAUDE.md — spring-security-ldap

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `ldap`.

## Module

Gradle project `:spring-security-ldap` (`spring-security-ldap.gradle`). Depends on
`spring-security-core` (`api`) plus a hard `api` dependency on `org.springframework.ldap:
spring-ldap-core` (Spring LDAP) — this module is Spring Security's authentication/authorities
layer built on top of Spring LDAP's context/template abstractions, not a from-scratch LDAP
client. `com.unboundid:unboundid-ldapsdk` is `optional` and backs the embedded test/dev LDAP
server (`server/UnboundIdContainer`); the older `ldapsdk:ldapsdk` is also `optional` and used
narrowly in `ppolicy/PasswordPolicyResponseControl` for password-policy control constants. Both
Jackson generations are `optional`. Has its own `integrationTest` source set/task (see the
`integrationTest { maxParallelForks = 1 }` block — LDAP integration tests must run serially,
presumably due to shared embedded-server state) including an `OpenLDAPIntegrationTestSuite`
against a real server, not just the embedded one.

Test scoping: `./gradlew :spring-security-ldap:test`, or for the LDAP-server integration suite:
`./gradlew :spring-security-ldap:integrationTest`.

## Package map

- Top level — `SpringSecurityLdapTemplate` (the main entry point for LDAP search/comparison
  operations, wrapping Spring LDAP's `LdapTemplate`), `DefaultSpringSecurityContextSource`
  (configures the LDAP connection/context), `LdapUsernameToDnMapper`/
  `DefaultLdapUsernameToDnMapper`, `LdapEncoder`, `LdapUtils`.
- `authentication/` — two distinct authentication strategies, both extending
  `AbstractLdapAuthenticator`: `BindAuthenticator` (authenticates by binding to the directory as
  the user with their supplied password — does not require read access to the password
  attribute) vs `PasswordComparisonAuthenticator` (authenticates by reading the user's password
  attribute and comparing it locally — requires the bind account to have read access to that
  attribute). `LdapAuthenticationProvider`/`AbstractLdapAuthenticationProvider` wire an
  `LdapAuthenticator` plus an authorities populator into a Spring Security `AuthenticationProvider`.
- `userdetails/` — `LdapAuthoritiesPopulator`/`DefaultLdapAuthoritiesPopulator`/
  `NestedLdapAuthoritiesPopulator` (resolves group membership → `GrantedAuthority`s, with the
  `Nested` variant following nested/recursive group membership), `LdapUserDetailsMapper`/
  `UserDetailsContextMapper` (maps a directory entry to a `UserDetails`), `LdapUserDetailsImpl`,
  `LdapUserDetailsManager` (a full `UserDetailsManager` backed by LDAP, supporting user
  creation/password change, not just lookup), `InetOrgPerson`/`Person` (`UserDetails`
  implementations modeling the standard `inetOrgPerson`/`person` LDAP object classes) with
  matching `InetOrgPersonContextMapper`/`PersonContextMapper`.
- `search/` — `LdapUserSearch`/`FilterBasedLdapUserSearch`, locating a user's DN by search
  filter rather than constructing it directly (needed when usernames don't map predictably to a
  DN pattern).
- `ppolicy/` — LDAP Password Policy (RFC-draft) control support:
  `PasswordPolicyAwareContextSource`, `PasswordPolicyControl`/`PasswordPolicyResponseControl`,
  `PasswordPolicyErrorStatus`/`PasswordPolicyException`, exposing directory-side password-expiry/
  lockout signals through authentication failures rather than generic bind errors.
- `server/` — `UnboundIdContainer` (an embeddable UnboundID-backed LDAP server, usable directly
  in application tests or even a dev profile, not just this module's own test suite) implementing
  `EmbeddedLdapServerContainer`.
- `jackson`/`jackson2` — dual Jackson mixin packages, same pattern as other modules.
- `aot` — AOT/native-image hints, same pattern as other modules.

## Working in this module

When adding or debugging authentication behavior, first determine which authenticator strategy is
in play (`BindAuthenticator` vs `PasswordComparisonAuthenticator`) — they have different
permission requirements against the directory and different failure modes, and a fix for one does
not necessarily apply to the other.
