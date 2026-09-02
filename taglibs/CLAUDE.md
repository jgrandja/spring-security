# CLAUDE.md — spring-security-taglibs

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `taglibs`.

## Module

Gradle project `:spring-security-taglibs` (`spring-security-taglibs.gradle`). Legacy JSP taglib
support — depends directly on `spring-security-acl`, `spring-security-core`, and
`spring-security-web` (`api`); `jakarta.servlet.jsp-api`/`jakarta.servlet-api` are `provided`
(this module produces classes consumed by a JSP container, not a servlet filter chain of its
own). Small and self-contained; no `optional` dependencies, no reactive equivalent (JSP has no
reactive story).

Test scoping: `./gradlew :spring-security-taglibs:test`.

## Tag classes map directly to `META-INF/security.tld`

`src/main/resources/META-INF/security.tld` declares the taglib's tags; each has a corresponding
Java class. When changing a tag's attributes/behavior, update both the `.tld` descriptor and the
tag class together — they're not generated from each other:

- `<authorize>` → `authz/JspAuthorizeTag` (extends `authz/AbstractAuthorizeTag`, implements
  `Tag`) — SpEL-based conditional rendering (`access="hasRole('ADMIN')"`), evaluated via a custom
  `EvaluationContext` (`PageContextVariableLookupEvaluationContext`, a nested class) that exposes
  JSP page-context variables to the expression alongside the usual Spring Security root object.
- `<authentication>` → `authz/AuthenticationTag` — renders a property of the current
  `Authentication`/principal.
- `<accesscontrollist>` → `authz/AccessControlListTag` (extends `TagSupport`, not
  `AbstractAuthorizeTag` — it's a separate lineage) — conditional rendering based on ACL
  `hasPermission` checks against `spring-security-acl`, distinct from `<authorize>`'s
  `AuthorizationManager`/SpEL-based check.
- `<csrfInput>` / `<csrfMetaTags>` → `csrf/CsrfInputTag` / `csrf/CsrfMetaTagsTag` (both extend
  `csrf/AbstractCsrfTag`) — emit a hidden CSRF input field or `<meta>` tags respectively, for
  pages not using a form-tag library with built-in CSRF support.

`TagLibConfig` (top-level) holds shared configuration constants used across the tag classes.

## Working in this module

This module is JSP-specific and largely frozen in scope — before adding a new tag, confirm
there's a real need rather than pointing users at the Java/Kotlin DSL or Thymeleaf/other
template-engine integrations instead, since new server-rendered UI work in this repo generally
does not target JSP.
