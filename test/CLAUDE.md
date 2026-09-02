# CLAUDE.md — spring-security-test

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `test`. Like
`kerberos-test`, this module's fixtures live in `src/main/java`, not `src/test/java` — it's
shipped as a real dependency for *other* modules' and applications' tests (e.g. `docs`'s test
suite, and most feature modules' own `testImplementation` on `spring-security-config`'s test-jar
transitively pull this in), not just used internally.

## Module

Gradle project `:spring-security-test` (`spring-security-test.gradle`). Depends on
`spring-security-core`, `spring-security-web`, and `spring-test` (`api`).
`spring-security-config`, `spring-security-oauth2-client`, `spring-security-oauth2-jose`, and
`spring-security-oauth2-resource-server` are all `optional` — OAuth2/OIDC/JWT test support (see
below) only activates if those modules are present on the consumer's classpath.
`spring-webmvc`/`spring-webflux`/`reactor-core` are also `optional`, gating the MockMvc vs.
WebTestClient integration respectively.

Test scoping: `./gradlew :spring-security-test:test`.

## Package map

- `context/support/` — the `@WithMockUser`/`@WithUserDetails`/`@WithAnonymousUser` mechanism:
  `@WithSecurityContext` is the meta-annotation contract (any custom annotation can be annotated
  with it), `WithSecurityContextFactory<A>` is the interface each annotation's handler implements
  (`WithMockUserSecurityContextFactory`, `WithUserDetailsSecurityContextFactory`,
  `WithAnonymousUserSecurityContextFactory`), and `WithSecurityContextTestExecutionListener` (a
  Spring TestContext Framework `TestExecutionListener`) is what actually finds the annotation on a
  test method/class and installs the resulting `SecurityContext` before the test runs.
  `ReactorContextTestExecutionListener` does the same for reactive tests, propagating the context
  onto the Reactor `Context` rather than a thread-local. `TestSecurityContextHolder`/
  `TestSecurityContextHolderStrategyAdapter` hold the context for the duration of the test.
- `context/annotation/SecurityTestExecutionListeners` — the meta-annotation that registers the
  above listeners on a test class (implicitly pulled in by `@WithMockUser` etc., but can be used
  directly for custom setups).
- `web/servlet/request/SecurityMockMvcRequestPostProcessors` — the single, large class behind
  `SecurityMockMvcRequestPostProcessors.user(...)`, `.jwt()`, `.oauth2Login()`, `.oidcLogin()`,
  `.oauth2Client()`, `.csrf()`, `.x509(...)`, etc. **OAuth2/OIDC/JWT MockMvc support is not a
  separate `oauth2` subpackage — it's bundled directly into this one class** as nested
  `RequestPostProcessor` implementations (`JwtRequestPostProcessor`,
  `OAuth2LoginRequestPostProcessor`, `OidcLoginRequestPostProcessor`,
  `OAuth2ClientRequestPostProcessor`, plus supporting test doubles like
  `TestOAuth2AuthorizedClientManager`/`TestOAuth2AuthorizedClientRepository`), only usable when
  the corresponding optional `oauth2-*` module is present. When adding MockMvc test support for a
  new authentication mechanism, this is the class to extend, not a new package.
- `web/servlet/response/` — `SecurityMockMvcResultMatchers`/`SecurityMockMvcResultHandlers`, for
  asserting on/printing the resulting `SecurityContext` after a MockMvc call.
- `web/servlet/setup/SecurityMockMvcConfigurers` — the `MockMvcBuilders.apply(springSecurity())`
  entry point that wires the Spring Security filter chain into MockMvc.
- `web/reactive/server/SecurityMockServerConfigurers` — the `WebTestClient` equivalent of the
  above, for reactive apps.
- `web/support/WebTestUtils` — shared helpers used by both the servlet and reactive integrations.
- `aot/` — AOT/native-image hints, same pattern as other modules.

## Working in this module

Prefer extending `SecurityMockMvcRequestPostProcessors`/`SecurityMockServerConfigurers` with a
matching pair (servlet + reactive) when adding support for a new authentication mechanism, since
consumers generally expect both testing styles to be supported together — check whether the
mechanism's own module (e.g. `oauth2-client`) already has an equivalent reactive test helper
before adding only one side.
