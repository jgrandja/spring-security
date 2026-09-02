# CLAUDE.md — buildSrc

Guidance specific to this directory. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `buildSrc`. Unlike
every other subdirectory covered by a `CLAUDE.md` in this repo, `buildSrc` is not a
`spring-security-*` feature module — it's Gradle's auto-included build-logic project, compiled and
put on the classpath of every other build script before the main build runs.

## What actually lives here vs. what's external

Several convention plugins applied throughout the repo (`io.spring.convention.root`,
`io.spring.convention.spring-module`, `io.spring.convention.checkstyle`, `io.spring.javaformat`,
`io.spring.nohttp`, `io.spring.security.release`) are **not defined in this `buildSrc`** — they
come from externally published Gradle plugins (`io.spring.javaformat:spring-javaformat-gradle-plugin`,
`io.spring.nohttp:nohttp-gradle`, `io.spring.security:spring-security-release-plugin`, etc.,
declared in `buildSrc/build.gradle`'s `dependencies` block). Only the plugins registered in this
file's own `gradlePlugin { plugins { ... } }` block are actually implemented in this directory:
`trang`, `locks`, `io.spring.convention.management-configuration`, `s101`,
`org.springframework.security.versions.verify-dependencies-versions`,
`org.springframework.security.check-expected-branch-version`. Don't go looking in this repo for
the source of a convention plugin without first checking whether it's one of these six — if not,
it's an external dependency; check `buildSrc/build.gradle`'s `implementation`/`libs.*` entries and
`gradle/libs.versions.toml` instead.

## Two different kinds of build logic live side by side

- `src/main/groovy/*.gradle` — **script plugins** (precompiled script plugins), applied by `id`
  matching the filename minus `.gradle` (e.g. `java-toolchain.gradle` → `apply plugin:
  'java-toolchain'`). These are what the root/module `CLAUDE.md` files reference as
  "buildSrc conventions": `java-toolchain.gradle`, `security-kotlin.gradle`,
  `security-nullability.gradle`, `compile-warnings-error.gradle`, `javadoc-warnings-error.gradle`,
  `test-compile-target-jdk25.gradle`.
- `src/main/java/**/*.java` — **binary plugins** (full `Plugin<Project>` classes), despite the
  `java` directory name these are compiled as Groovy, not Java: the `sourceSets` block in
  `build.gradle` redirects `java.srcDirs` to empty and routes `src/main/java` through the
  `groovy` source set instead. This is a historical layout quirk, not a build error — don't
  "fix" it by moving files to `src/main/groovy` or renaming the directory.

## Locally-implemented plugins

- `trang/` — `TrangPlugin` + `RncToXsd` task: the implementation behind
  `./gradlew :spring-security-config:rncToXsd` (see [config/CLAUDE.md](../config/CLAUDE.md)),
  wrapping the Trang RELAX NG compact syntax → XSD converter.
- `lock/` — `GlobalLockPlugin`/`GlobalLockTask`, registered as the `locks` plugin ID, applied in
  the root `build.gradle`.
- `s101/` — `S101Plugin` and supporting `S101Install`/`S101Configure`/`S101Configurer`/
  `S101PluginExtension` — Structure101 architecture-analysis integration; config templates live in
  `src/main/resources/s101/`.
- `org/springframework/security/convention/versions/` — `VerifyDependenciesVersionsPlugin`
  (registered as `org.springframework.security.versions.verify-dependencies-versions`, applied at
  the root) plus its helpers `FileUtils`/`TransitiveDependencyLookupUtils`.
- `org/springframework/security/CheckExpectedBranchVersionPlugin` — registered as
  `org.springframework.security.check-expected-branch-version`; enforces that `gradle.properties`'
  `version` matches what's expected for the current branch (guards against forgetting to bump
  version after a branch/release operation).
- `org/springframework/gradle/` — smaller standalone utility plugins not registered in
  `gradlePlugin { plugins { ... } }` (applied directly by class, if at all): `classpath/`,
  `maven/`, `propdeps/`, `xsd/`, plus top-level `CopyPropertiesPlugin`.

## Testing

`./gradlew :buildSrc:test` runs this project's own JUnit tests (note: run from the repo root, or
`cd buildSrc && ../gradlew test`, since `buildSrc` has its own implicit build). Tests can be
skipped repo-wide via `-PbuildSrc.skipTests` (checked directly in `build.gradle`'s `test` task
configuration) — useful when iterating on the main build and the convention-plugin tests aren't
relevant to the change at hand.
