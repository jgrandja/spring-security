# CLAUDE.md — spring-security-oauth2-jose

Guidance specific to this module. See the root [CLAUDE.md](../../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `oauth2-jose`.

## Module

Gradle project `:spring-security-oauth2-jose` (`oauth2/oauth2-jose/spring-security-oauth2-jose.gradle`).
Depends on `spring-security-core` and `spring-security-oauth2-core` (`api`). Unlike most feature
modules, `com.nimbusds:nimbus-jose-jwt` is a **hard `api` dependency here, not optional** — this
module is essentially a thin, opinionated wrapper around Nimbus's JOSE/JWT support, so there is no
"non-Nimbus" code path to keep isolated (contrast with `oauth2-core`, where Nimbus usage is
confined to one class). `spring-webflux`/`reactor-core` are `optional`, gating only the reactive
decoder classes.

Test scoping: `./gradlew :spring-security-oauth2-jose:test`, or with a filter:
`./gradlew :spring-security-oauth2-jose:test --tests "*NimbusJwtDecoder*"`.

## Package map

- `jwt/` — nearly everything in this module lives at this one package level (no further
  subpackages): the `Jwt`/`JwtClaimsSet`/`JwtClaimNames` model, `JwtDecoder`/`JwtEncoder`
  interfaces, their Nimbus-backed implementations (`NimbusJwtDecoder`, `NimbusJwtEncoder`), decoder
  factories/builders (`JwtDecoders`, `JwtDecoderFactory`, `SupplierJwtDecoder`), validators
  (`JwtValidators`, `JwtIssuerValidator`, `JwtAudienceValidator`, `JwtTimestampValidator`,
  `JwtIssuedAtValidator`, `JwtTypeValidator`, `X509CertificateThumbprintValidator`,
  `DPoPProofReplayValidator`), and DPoP support (`DPoPProofContext`, `DPoPProofJwtDecoderFactory`).
- `jose/` — the small, generic algorithm-identifier types shared between JWS/JWE contexts:
  `JwaAlgorithm` at the top, with `jose/jws` holding `JwsAlgorithm`/`SignatureAlgorithm`/
  `MacAlgorithm`/`JwsAlgorithms`. These are framework-agnostic constants, not tied to Nimbus.

## Servlet/reactive: interleaved by class name, like `core` — not split by package

Unlike `web`, this module does not split servlet vs. reactive into separate packages. Blocking and
reactive variants sit side by side in `jwt/`, distinguished only by a `Reactive` prefix (following
`core`'s convention, not `web`'s): `JwtDecoder`/`ReactiveJwtDecoder`, `NimbusJwtDecoder`/
`NimbusReactiveJwtDecoder`, `JwtDecoders`/`ReactiveJwtDecoders`, `JwtDecoderFactory`/
`ReactiveJwtDecoderFactory`, `SupplierJwtDecoder`/`SupplierReactiveJwtDecoder`,
`JwtDecoderProviderConfigurationUtils`/`ReactiveJwtDecoderProviderConfigurationUtils`. There is no
reactive equivalent of `JwtEncoder`/`NimbusJwtEncoder` — token issuance in this module is
blocking-only; reactive support here is decode/verification only (consumed by
`oauth2-resource-server`'s reactive filters), not encoding (used by
`oauth2-authorization-server`, which is not reactive).

## Nimbus is the implementation backbone, not an implementation detail to hide

Because Nimbus is a hard dependency, this module's public API deliberately exposes Nimbus-shaped
concepts where useful rather than hiding them behind an abstraction — e.g. `NimbusJwtEncoder`
accepts a Nimbus `JWKSource`/`JWKSet`/`JWKSourceBuilder` directly. When adding decoder/encoder
configuration options, check whether Nimbus already exposes the equivalent knob before introducing
a new Spring Security abstraction for it.
