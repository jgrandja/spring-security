# CLAUDE.md — spring-security-saml2-service-provider

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to
`saml2-service-provider`.

## Module

Gradle project `:spring-security-saml2-service-provider`
(`saml2/saml2-service-provider/spring-security-saml2-service-provider.gradle`). Implements SAML
2.0 **Service Provider** support only (SP-initiated SSO/login, SP-side single logout) — not
Identity Provider support. Base package is nested:
`org.springframework.security.saml2.provider.service.*`, with shared vocabulary
(`Saml2X509Credential`, `Saml2Error`, `Saml2ParameterNames`, `OpenSamlInitializationService`) in
a separate top-level `org.springframework.security.saml2.core`. Depends on
`spring-security-web` (`api`); `jakarta.servlet-api` is `provided`. Servlet-only — no reactive
counterpart, unlike `web` or `oauth2-client`. `spring-jdbc` is `optional`, gating
`JdbcAssertingPartyMetadataRepository`.

Test scoping: `./gradlew :spring-security-saml2-service-provider:test`. There is a **second**
OpenSAML-5-specific test task, `opensaml5Test` (see below) — a fix touching OpenSAML-facing code
should be verified against both.

## Dual OpenSAML version support — a real, unusual build mechanism, not an accident

This module compiles against **two different major versions of OpenSAML at once** and ships a
single combined jar. This shows up as duplicated-looking source files — do not "clean up" the
duplication without understanding the mechanism first:

- The main `api project(':spring-security-web')`/OpenSAML dependency in the `dependencies` block
  (`org.opensaml:opensaml-saml-api`/`opensaml-saml-impl`) is one OpenSAML major version, compiled
  as ordinary `src/main/java`.
- A second, separate source set `opensaml5Main` (source at `src/opensaml5Main/java`, its own
  `opensamlFiveMain` dependency configuration pointing at `libs.org.opensaml.opensaml5.saml.api`)
  compiles OpenSAML-5-specific implementation code against the newer major version. Its own test
  source set/task is `opensaml5Test` / `./gradlew :spring-security-saml2-service-provider:opensaml5Test`.
  The final jar bundles both (`jar { from sourceSets.opensaml5Main.output }`).
- All actual OpenSAML API usage is isolated behind a single package-private SPI,
  `saml2.internal.OpenSamlOperations` (`build`/`deserialize`/`serialize`/`withSigningKeys`/
  `withVerificationKeys`/`withDecryptionKeys`), with one implementation per OpenSAML major version
  (e.g. `OpenSaml5Template` under `src/opensaml5Main/java/.../internal/`). Public API classes
  historically had OpenSAML-version-suffixed names (`OpenSaml4AuthenticationProvider`,
  `OpenSaml5AuthenticationProvider`) — those were **removed**; the public API is now
  version-agnostic (`Saml2AuthenticationProvider`-style names), with `Base*` classes (e.g.
  `BaseOpenSamlAuthenticationProvider`, `BaseOpenSamlAuthenticationRequestResolver`,
  `BaseOpenSamlLogoutRequestValidator`, `BaseOpenSamlAssertingPartyMetadataRepository`,
  `BaseOpenSamlMetadataResolver`) doing the OpenSAML-facing work through `OpenSamlOperations`.
- **The build file's `sourceSets.configureEach` block literally copies
  `saml2/internal/OpenSamlOperations.java` and `Saml2Utils.java` into each consuming package**
  (`authentication`, `authentication/logout`, `metadata`, `web`, `web/authentication`,
  `web/authentication/logout`, `registration`), rewriting the package declaration via a text
  filter — and does this for **both** the `main` and `opensaml5Main` source sets. These copies are
  committed to git (not gitignored build output), so `authentication/OpenSamlOperations.java`,
  `metadata/OpenSamlOperations.java`, etc., all genuinely exist as tracked, byte-for-byte-identical
  (modulo package name) files. **`saml2/internal/OpenSamlOperations.java` /
  `saml2/internal/Saml2Utils.java` are the single source of truth — edit those, then re-run the
  build so the copy task regenerates the per-package duplicates.** Editing a duplicated copy
  directly will be silently overwritten.

## Package map

- `core/` — `Saml2X509Credential`, `Saml2Error`, `Saml2ParameterNames`,
  `OpenSamlInitializationService`.
- `provider/service/registration/` — the RP configuration/metadata model:
  `RelyingPartyRegistration`, `RelyingPartyRegistrationRepository` (+ `InMemory*`/`Caching*`/
  `Iterable*` implementations), `RelyingPartyRegistrations` (builders that load from an IdP
  metadata URL/file), `AssertingPartyMetadata` (the IdP-side metadata contract, implemented by
  `OpenSamlAssertingPartyDetails`), `AssertingPartyMetadataRepository` +
  `JdbcAssertingPartyMetadataRepository`, `Saml2MessageBinding`.
- `provider/service/authentication/` — `Saml2AuthenticationToken`, `Saml2Authentication`,
  `Saml2AssertionAuthentication`, `Saml2AuthenticatedPrincipal`/`DefaultSaml2AuthenticatedPrincipal`,
  `Saml2AuthenticationException`, `AbstractSaml2AuthenticationRequest` +
  `Saml2PostAuthenticationRequest`/`Saml2RedirectAuthenticationRequest`; nested
  `authentication/logout` for SAML Single Logout request/response types and their validators.
- `provider/service/web/` and `web/authentication/` — the servlet integration:
  `Saml2WebSsoAuthenticationRequestFilter` (builds/sends the AuthnRequest),
  `Saml2WebSsoAuthenticationFilter` (handles the ACS/assertion-consumer callback),
  `Saml2AuthenticationTokenConverter`, `Saml2MetadataFilter`,
  `Saml2AuthenticationRequestRepository` (+ session/cache-backed implementations),
  `RelyingPartyRegistrationResolver`/`DefaultRelyingPartyRegistrationResolver`; nested
  `web/authentication/logout` for the SLO servlet flow and `web/metadata` for the metadata
  endpoint.
- `provider/service/metadata/` — SP/IdP metadata resolution (`Saml2MetadataResolver`,
  `Saml2MetadataResponseResolver`), distinct from — but overlapping in filename with —
  `web/metadata` (both have a `RequestMatcherMetadataResponseResolver`-style class; check the
  full package path, not just the class name).
- `jackson`/`jackson2` — dual Jackson packages, but asymmetric: `jackson2`'s
  `Saml2Jackson2Module` is `@Deprecated(forRemoval = true)` — new code should only need `jackson`
  (Jackson 3).
- `aot/hint/Saml2RuntimeHints` — AOT hints, same pattern as other modules.
