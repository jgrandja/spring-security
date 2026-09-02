# CLAUDE.md — spring-security-docs

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `docs`. Unlike
every feature module, this is the Antora/Asciidoctor reference documentation, built via
`./gradlew :spring-security-docs:antora` (output published to `docs/build/site`).

## Module

Gradle project `:spring-security-docs` (`spring-security-docs.gradle`). Applies the `org.antora`
and `io.spring.antora.generate-antora-yml` plugins plus `io.spring.convention.docs`. Has real
`testImplementation` dependencies on most feature modules (`spring-security-config`,
`spring-security-test`, `spring-security-oauth2-client`, `spring-security-oauth2-resource-server`,
`spring-security-messaging`, `spring-security-webauthn`, ...) — this module compiles and runs
actual tests, it isn't just prose.

## Code snippets are compiled/tested source, not text embedded in `.adoc` files

Documentation code examples live under `src/test/java` (and `src/test/kotlin`), organized as
`org/springframework/security/docs/{servlet,reactive}/<topic>/*.java`, wrapped with Asciidoctor
tag comments:

```java
// tag::snippet[]
... example code ...
// end::snippet[]
```

`.adoc` pages under `modules/ROOT/pages/` pull these in via `include::.../SomeFile.java[tag=snippet]`
rather than duplicating code inline. This means a documentation code example is compiled and can
have real JUnit assertions run against it (`./gradlew :spring-security-docs:test` runs them like
any other module's tests) — a snippet shown in the reference docs is guaranteed to compile and
(if it has a corresponding `*Tests.java`) actually behave as documented. When updating a
documented API, update the tagged source file under `src/test/java/.../docs/...`, not the `.adoc`
prose directly, and re-run this module's tests rather than just eyeballing the doc page.

## Content layout mirrors the repo's own servlet/reactive split

`modules/ROOT/pages/servlet/` and `modules/ROOT/pages/reactive/` are separate top-level page trees
(each with their own `authentication/`, `authorization/`, `configuration/`, `exploits/`,
`integrations/`, `oauth2/`, `test/` subdirectories) — consistent with the servlet/reactive
duality documented in the root CLAUDE.md, but here expressed as parallel documentation sections
rather than parallel code. `modules/ROOT/pages/migration/` and `migration-8/` hold
version-upgrade guides; `modules/ROOT/pages/native-image/` covers AOT/native-image support (see
the `aot` packages referenced in several module `CLAUDE.md` files). `modules/ROOT/partials/` and
`modules/ROOT/examples/` hold reusable `.adoc` fragments and example resources
(`examples/kerberos/` etc.) included from multiple pages.

## Other content

- `articles/src/docbook/` — legacy DocBook-format content (`codebase-structure.xml`), separate
  from the Antora/Asciidoctor `modules/` tree; not part of the published Antora site build.
- `docs/bin/` is stale/gitignored build output — do not treat anything under it as source.

## Building and previewing

`./gradlew :spring-security-docs:antora` builds the full site to `docs/build/site`; per the root
README, the actual Antora playbook/build process is described in the repo's separate
`docs-build` branch. Use `./gradlew :spring-security-docs:test` to run just the snippet tests
without a full Antora build when iterating on example code.
