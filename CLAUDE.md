# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Security — security services (authentication, authorization, protection against common
attacks) for the Spring IO Platform. Requires Java 17+ at runtime; the build itself uses a newer
JDK toolchain (see below). Licensed Apache 2.0.

## Build System

Gradle multi-module build (~40 modules), wrapper-invoked: `./gradlew`.

- JDK: pinned via `.sdkmanrc` (`java=25-librca`). Run `sdk env` (via sdkman) before invoking
  Gradle directly if you don't already have a matching JDK active.
- Module wiring is dynamic: `settings.gradle` scans the whole tree for `*.gradle`/`*.gradle.kts`
  files and registers each as a project named after the *build file*, not the directory. E.g.
  `oauth2/oauth2-client/spring-security-oauth2-client.gradle` becomes project
  `:spring-security-oauth2-client`. Use `./gradlew projects` if unsure of a module's Gradle name.
- Common commands:
  - `./gradlew build` — compile, test, build all JARs/zips/docs (slow, whole repo).
  - `./gradlew publishToMavenLocal` — install all `spring-*.jar` locally.
  - `./gradlew :spring-security-core:test` — test a single module (prefer this over root
    `build`/`test` for iterative work).
  - `./gradlew :spring-security-oauth2-client:test --tests "*OidcSomething*"` — run a single test
    class/method.
  - `./gradlew format` — apply code formatting (Spring JavaFormat).
  - `./gradlew check` — run checkstyle + formatting verification + tests for changed modules.
  - `./gradlew format && ./gradlew check` — required before submitting a PR (see
    CONTRIBUTING.adoc).
  - `./gradlew :spring-security-config:rncToXsd` — regenerate XSD after editing an RNC file
    (never hand-edit the XSD).
  - `./gradlew :spring-security-docs:antora` — build reference docs (Antora/Asciidoctor), output
    to `docs/build/site`.
  - `./gradlew tasks` — discover more tasks.

### Formatting rules

Enforced by `.editorconfig` and checkstyle (via `io.spring.javaformat`), not just convention:
- Tabs for `.java`/`.xml`/`.gradle`, indent size 4, continuation indent 8.
- Max line length 120, LF line endings, trailing whitespace trimmed, final newline required.
- Import order is a known exception `./gradlew format` does not fix — check manually.
- Streams are noticeably slower than `for` loops in this codebase; avoid them on hot paths
  (reviewers will ask for a `for` loop instead — see CONTRIBUTING.adoc).

### IDE setup

- IntelliJ: opens with no special steps.
- Eclipse / VS Code: run `./gradlew cleanEclipse eclipse` to generate `.classpath`/`.project`
  metadata, then import the repo root. VS Code's `.vscode/settings.json` disables automatic
  Gradle import — do not import via the Gradle for Java extension; use the generated Eclipse
  metadata instead.

## Architecture

### Module layout

Each top-level directory (`core`, `web`, `config`, `crypto`, `acl`, `cas`, `ldap`, `messaging`,
`rsocket`, `saml2`, `oauth2`, `kerberos`, `webauthn`, `taglibs`, `data`, `aspects`, `test`, `itest`,
...) is one or more Gradle modules, each producing its own `spring-security-*` JAR. Modules with
several independently-versioned pieces (e.g. `oauth2`, `saml2`, `kerberos`, `itest`) nest further:
each leaf subdirectory has its own `spring-security-<name>.gradle` file.

Dependency direction generally flows: `spring-security-crypto` → `spring-security-core` →
`spring-security-web` (all hard `api` dependencies) → feature modules (`oauth2-*`, `saml2-*`,
`cas`, `ldap`, `acl`, ...), which depend on `core`/`web` directly, not on `config`.
`spring-security-config` sits apart from this chain rather than in the middle of it: it has only
one hard dependency (`core`), and pulls in `web` plus nearly every feature module (`access`,
`ldap`, `messaging`, `saml2-service-provider`, `oauth2-*`, `rsocket`, `webauthn`, `data`) as
`optional`, so it can wire their DSL/`SecurityFilterChain` configuration without forcing them onto
every consumer's classpath. So dependencies point *into* `config`, not through it — feature
modules don't "plug into `config`"; `config` optionally plugs into them. `spring-security-test`
provides test support consumed across modules. `spring-security-bom` and
`spring-security-dependencies` centralize version alignment (the `bom` module has
formatting/checkstyle disabled — it's metadata only).

### Servlet vs. reactive duality

Most security concerns are implemented twice: once for servlet (blocking) stacks and once for
WebFlux (reactive) stacks. There is no single consistent pattern for where the reactive
counterpart lives — it varies by module (and even by package within a module), so don't assume a
`servlet`/`reactive` sibling pair exists just because one does elsewhere:
- `core`: interleaved in the same package, distinguished by a `Reactive`-prefixed/suffixed class
  name (e.g. `AuthenticationManager` / `ReactiveAuthenticationManager`).
- `web`: default/unqualified packages are servlet; `web.server` is a full reactive mirror of the
  whole module; `web.servlet`/`web.reactive` are narrow framework-integration glue, not mirrors.
- `config`: inconsistent per area — see [config/CLAUDE.md](config/CLAUDE.md) for the actual
  locations (e.g. the reactive DSL entry point `ServerHttpSecurity` lives in `config/web/server`,
  a sibling of `annotation`, not inside `annotation/web/reactive`).
- `oauth2-client`/`oauth2-resource-server`: reactive clients/filters live alongside blocking ones.

When implementing or fixing something, check whether an equivalent
reactive or servlet counterpart also needs the change.

### Kotlin DSL

Alongside the Java config DSL there's a parallel Kotlin DSL for security configuration (governed by
the `security-kotlin.gradle` buildSrc convention applied per-module). Changes to a Java DSL
configurer often need a matching Kotlin DSL update in the same module's `src/main/kotlin` tree.

### Samples and integration tests

`itest/` holds integration tests that exercise multiple modules together (`context`, `web`)
distinct from each module's own unit tests. Sample apps referenced in docs live in a separate
branch/repo (`samplesBranch` in `gradle.properties`), not in this tree.

### buildSrc conventions

`buildSrc/src/main/groovy` defines custom Gradle conventions applied across modules, notably:
`java-toolchain.gradle` (JDK toolchain setup), `security-kotlin.gradle` (Kotlin support per
module), `security-nullability.gradle`, `compile-warnings-error.gradle`,
`javadoc-warnings-error.gradle`, `test-compile-target-jdk25.gradle`. Cross-module build behavior
changes usually belong here rather than in individual module `.gradle` files.

## Contribution conventions

See `CONTRIBUTING.adoc` and `RELEASE.adoc` for full detail; key points that affect how changes
should be structured:

- Target branch: match the milestone (e.g. `5.8.x` for a `5.8.3` fix); default to `main` if no
  milestone. Fixes are forward-ported to `main` and other applicable branches after merge.
- Commits: DCO `Signed-off-by` trailer required on every commit. Subject line ≤55 chars,
  body lines ≤72 chars, imperative tense ("Fix" not "Fixes"/"Fixed"), reference the issue as
  `Closes gh-<number>`.
- Copyright headers: when editing a file with an `2002-20xx` header, bump the end year to the
  current year.
- New public API on `main`: add `@since` Javadoc.
- XSD changes: edit the RNC source and regenerate via
  `./gradlew :spring-security-config:rncToXsd` — never hand-edit the generated XSD.
- Add JUnit tests and documentation for behavioral changes.
