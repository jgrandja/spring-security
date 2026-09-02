# CLAUDE.md — spring-security-config

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `config`, and
corrects the root file's simplified description of the servlet/reactive DSL split.

## Module

Gradle project `:spring-security-config` (`spring-security-config.gradle`). Only hard
`spring-security-*` dependency is `api project(':spring-security-core')` — a comment in the
build file warns not to add other compile-time `spring-security-*` dependencies here, as it
breaks tooling. Every other feature module this config layer supports —
`spring-security-web`, `-access`, `-data`, `-ldap`, `-messaging`, `-saml2-service-provider`,
`-oauth2-client`, `-oauth2-jose`, `-oauth2-resource-server`, `-oauth2-authorization-server`,
`-rsocket`, `-webauthn` — is `optional`, along with `spring-webmvc`, `spring-webflux`,
`spring-websocket`, `reactor-core`, `aspectjweaver`. `jakarta.servlet-api` is `provided`. This
module is effectively the wiring layer that ties feature modules together only if they're present
on the classpath, so any code referencing an optional module's types must fail gracefully (or not
load at all) when that module is absent — don't assume `web`/`oauth2-*`/`saml2-*` etc. are there.

Test scoping: `./gradlew :spring-security-config:test`, or with a filter:
`./gradlew :spring-security-config:test --tests "*HttpSecurity*"`.

## Top-level packages

`annotation` (Java config DSL — the bulk of the module), `authentication`
(`AuthenticationManagerBuilder`/global-authentication wiring), `core` (shared non-annotation
utilities, e.g. `GrantedAuthorityDefaults`, `core/userdetails` factory beans), `crypto`
(`PasswordEncoder`-related config support), `debug` (`@EnableWebSecurity(debug=true)` support),
`http`, `ldap`, `method`, `oauth2`, `observation`, `provisioning`, `saml2`, `web`, `websocket`,
`aot`. `http`, `ldap`, `websocket`, `oauth2` (client), and `saml2` at this top level are legacy
**XML namespace** parsers (`*BeanDefinitionParser`), not Java config — see below.

## The servlet/reactive split is inconsistent across the module — do not assume symmetry

Unlike the root file's simplified framing, there is no single clean `servlet` vs `reactive`
package pair. Each area of the module splits differently:

- **`annotation/web/{servlet,reactive,socket}` are NOT parallel/full packages.**
  `annotation/web/servlet` contains only `servlet/configuration/WebMvcSecurityConfiguration` — a
  thin wrapper, not a home for servlet DSL logic. `annotation/web/reactive` (flat) holds the real
  reactive bootstrap classes: `EnableWebFluxSecurity`, `WebFluxSecurityConfiguration`,
  `ServerHttpSecurityConfiguration`. `annotation/web/socket` is WebSocket-specific
  (`EnableWebSocketSecurity`), a third sibling, not nested under either.
- **The servlet DSL builders/configurers are NOT under `annotation/web/servlet`** — they live in
  the *shared-looking* `annotation/web/builders` (`HttpSecurity`, `WebSecurity`,
  `FilterOrderRegistration`) and `annotation/web/configurers` (~25 `*Configurer` classes:
  `FormLoginConfigurer`, `CsrfConfigurer`, `SessionManagementConfigurer`, etc., plus
  `configurers/oauth2`, `configurers/ott`, `configurers/saml2`). These packages are servlet-only in
  practice even though nothing in the package name says so.
  - Shared builder infrastructure used by `HttpSecurity`/`WebSecurity`:
    `SecurityBuilder`/`AbstractSecurityBuilder`/`AbstractConfiguredSecurityBuilder` and
    `SecurityConfigurer`/`SecurityConfigurerAdapter` at the top of `annotation`.
- **The reactive DSL entry point lives elsewhere entirely: `config/web/server/ServerHttpSecurity`**
  — a top-level sibling package to `annotation`, not nested inside it. `ServerHttpSecurity` does
  **not** extend the `AbstractConfiguredSecurityBuilder`/`SecurityConfigurer` hierarchy that
  `HttpSecurity` uses — it's an independently-built fluent API. Its nested spec classes (e.g.
  `AuthorizeExchangeSpec`) play the role `*Configurer` classes play on the servlet side, extending
  `AbstractServerWebExchangeMatcherRegistry` in the same `web/server` package.
- **`annotation/method` is intentionally unsplit** — `EnableMethodSecurity` and
  `EnableReactiveMethodSecurity` (plus legacy `EnableGlobalMethodSecurity`,
  `GlobalMethodSecurityConfiguration`, `PrePostMethodSecurityConfiguration`,
  `ReactiveMethodSecurityConfiguration`, `Jsr250MethodSecurityConfiguration`,
  `SecuredMethodSecurityConfiguration`) live side by side under
  `annotation/method/configuration`, distinguished by class name rather than package — following
  the same convention as the `core` module's `Reactive`-prefixed classes.

When touching servlet or reactive HTTP security config, check the actual location of its
counterpart per the above — do not assume a `servlet`/`reactive` sibling package exists.

## Kotlin DSL

`config/src/main/kotlin/org/springframework/security/config/` has two packages that mirror the
Java split above: `annotation/web` for the servlet DSL (`HttpSecurityDsl.kt`, `FormLoginDsl.kt`,
`CsrfDsl.kt`, `AuthorizeHttpRequestsDsl.kt`, plus `headers`, `oauth2`, `saml2`, `session`
subpackages) and `web/server` for the reactive DSL (`ServerHttpSecurityDsl.kt`,
`ServerCsrfDsl.kt`, `AuthorizeExchangeDsl.kt`, ...). Naming convention: `<Concern>Dsl.kt` for
servlet, `Server<Concern>Dsl.kt` for reactive — roughly 1:1 with the Java configurers/specs. A new
servlet `*Configurer` or reactive spec class usually needs a matching Kotlin DSL class in the
corresponding package.

## Legacy XML namespace support vs. Java DSL

`http`, `ldap`, `websocket`, `oauth2/client`, and `saml2` at the top level are `*BeanDefinitionParser`
classes for the legacy Spring XML `<http>`/`<ldap-server>`/`<websocket-message-broker>` namespaces
— a parallel, older configuration surface to the Java DSL in `annotation/web`. `http` is the
largest (`HttpSecurityBeanDefinitionParser`, `HttpConfigurationBuilder`,
`FilterChainBeanDefinitionParser`, plus per-feature parsers like `CsrfBeanDefinitionParser`,
`OAuth2LoginBeanDefinitionParser`, `Saml2LoginBeanDefinitionParser`). These parsers construct
equivalent filter chains to what `HttpSecurity` + configurers build programmatically — a behavior
change to a `*Configurer` may need a corresponding change to its XML parser counterpart in `http`
(or its own package) to keep the two configuration surfaces in sync.

## XSD/RNC (versioned schema files)

The XML namespace is defined by RNC source files under
`config/src/main/resources/org/springframework/security/config/`, one pair of
`spring-security-<version>.rnc`/`.xsd` per release since 3.1 (versions before 3.1 only have the
`.xsd`), plus a shared `spring-security.xsl`. Per the root CLAUDE.md: never hand-edit an `.xsd` —
edit the matching `.rnc` and run `./gradlew :spring-security-config:rncToXsd`.

## AOT hints

`config/aot/hint` is much thinner than the AOT support in `core`/`web`: only
`WebSecurityConfigurationRuntimeHints`, `WebMvcSecurityConfigurationRuntimeHints`, and
`OAuth2LoginRuntimeHints` — servlet/MVC-focused only, no reactive AOT hints class exists here.
