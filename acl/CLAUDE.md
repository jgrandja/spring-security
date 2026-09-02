# CLAUDE.md — spring-security-acl

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `acl`.

## Module

Gradle project `:spring-security-acl` (`spring-security-acl.gradle`, directory `acl/`). **The base
package is `org.springframework.security.acls` (plural) — not `acl`.** Depends on
`spring-security-core`, plus `spring-jdbc`/`spring-tx` as hard `api` dependencies (this module
assumes a relational database for ACL storage; there's no in-memory-only implementation). No
`optional` dependencies — this is a small, self-contained module implementing domain object
security (per-instance, per-principal/role permissions), independent of the request/method
security machinery in `core`/`web`/`access`.

Test scoping: `./gradlew :spring-security-acl:test`, or with a filter:
`./gradlew :spring-security-acl:test --tests "*AclImpl*"`.

## Package map (contract vs. implementation vs. persistence)

- `model/` — the ACL domain contract, no implementations: `Acl`/`MutableAcl`/`AuditableAcl`/
  `OwnershipAcl`, `AccessControlEntry`/`AuditableAccessControlEntry`, `Sid` (security identity —
  a principal or authority, not an OS/Windows SID), `ObjectIdentity` (identifies the secured
  domain object instance), `Permission`, `AclService`/`MutableAclService`, `AclCache`,
  `PermissionGrantingStrategy`, `SidRetrievalStrategy`/`ObjectIdentityRetrievalStrategy`/
  `ObjectIdentityGenerator`.
- `domain/` — default implementations of the `model/` contracts: `AclImpl`, `PrincipalSid`/
  `GrantedAuthoritySid`, `ObjectIdentityImpl`, `AccessControlEntryImpl`, `BasePermission`/
  `AbstractPermission`/`CumulativePermission` (permissions are bitmasks — `CumulativePermission`
  combines several into one), `DefaultPermissionFactory`/`PermissionFactory`,
  `DefaultPermissionGrantingStrategy`, `AclAuthorizationStrategy`/`AclAuthorizationStrategyImpl`
  (governs who may *modify* an ACL, distinct from who may *access* the secured object),
  `SpringCacheBasedAclCache`, `AuditLogger`/`ConsoleAuditLogger`.
- `jdbc/` — the (only) persistence implementation: `JdbcAclService`/`JdbcMutableAclService`,
  `LookupStrategy`/`BasicLookupStrategy` (batches/optimizes ACL + inheritance-chain lookups from
  the database), `AclClassIdUtils`.
- Top level — `AclPermissionEvaluator` (implements `core`'s `PermissionEvaluator` from
  `core.access`, bridging SpEL `hasPermission(...)` expression checks to this module's `AclService`)
  and `AclPermissionCacheOptimizer` (implements `core`'s `PermissionCacheOptimizer` to batch-load
  ACLs for a collection before per-element permission checks, avoiding N+1 lookups).
- `aot/hint/AclRuntimeHints` — AOT/native-image hints for this module, following the pattern in
  `core`/`web`/`config`.

## Relationship to other modules

- `access` module's `vote/AbstractAclVoter` is the integration point on the *voting-based* legacy
  method-security side (see [access/CLAUDE.md](../access/CLAUDE.md)) — it's declared there because
  `access` only has an `optional` dependency on this module, whereas this module has no reverse
  dependency on `access`.
- `AclPermissionEvaluator`/`AclPermissionCacheOptimizer` are how this module plugs into the SpEL
  `hasPermission()` expressions evaluated by `core.access.expression`/`core.authorization.method` —
  wire an `AclPermissionEvaluator` bean if a project wants `hasPermission()` in
  `@PreAuthorize`/`@PostAuthorize` to consult ACLs rather than a custom `PermissionEvaluator`.
