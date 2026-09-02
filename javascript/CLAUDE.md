# CLAUDE.md — spring-security-javascript

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — those Java/Gradle conventions (JDK toolchain,
checkstyle, `.editorconfig`) mostly do not apply here. This is the one module in the repo with a
Node/npm toolchain instead of a Java source tree.

## Module

Gradle project `:spring-security-javascript` (`spring-security-javascript.gradle`), built via the
`com.github.node-gradle.node` plugin (Node 20.17.0, auto-downloaded) rather than any Java plugin.
Despite the generic module name, its actual content is **WebAuthn/passkey client-side JS only**
(`package.json` name: `@springprojects/spring-security-webauthn`) — not CSRF or other web-security
JS. `npm run check` (tests + lint) is wired into the Gradle `check` task via
`tasks.named('check') { dependsOn 'npm_run_check' }`.

Common commands (run from `javascript/`, or via Gradle from the repo root):
- `npm test` (or `./gradlew :spring-security-javascript:npm_run_check`) — run the Mocha test
  suite (`test/*.test.js`).
- `npm run lint` / `npm run format` — ESLint (`eslint.config.js`), with `format` running lint
  with `--fix`.
- `npm run assemble` — bundles `lib/index.js` with esbuild into
  `build/dist/spring-security-webauthn.js`.

## How this plugs into `spring-security-web`

This module exposes its build output as a custom outgoing Gradle configuration named
`javascript` (`configurations { javascript { canBeConsumed = true; canBeResolved = false } }`),
backed by the `build/dist` directory and built via the `npm_run_assemble` task. The `web` module
(see [web/CLAUDE.md](../web/CLAUDE.md)) declares `javascript project(path:
':spring-security-javascript', configuration: 'javascript')` and syncs that directory into its
own `build/spring-security-javascript` at build time — that's how the bundled WebAuthn JS ends up
available to `spring-security-web`'s default login/registration pages. When changing anything
under `lib/`, `web`'s sync task needs a rebuild to pick it up; don't edit synced JS under `web`'s
`build/` directory.

Note: the Gradle `dist` task (a separate `Zip` task, not the one `web` actually consumes) packages
`build/dist/spring-security.js`, but `npm run assemble` currently produces
`build/dist/spring-security-webauthn.js` — a filename mismatch. Verify before relying on the
`dist` zip task if you need it; the `javascript` configuration `web` actually depends on points at
the whole `build/dist` directory, not this specific filename, so it isn't affected by the
mismatch.

## Layout

- `lib/` — the actual WebAuthn client implementation: `webauthn-core.js`, `webauthn-login.js`,
  `webauthn-registration.js`, plus small supporting utilities (`base64url.js`, `http.js`,
  `abort-controller.js`), re-exported from `index.js`.
- `test/` — Mocha tests, one file per `lib/` module, plus `bootstrap.js` for test setup.
