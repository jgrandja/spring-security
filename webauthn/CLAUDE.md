# CLAUDE.md — spring-security-webauthn

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `webauthn`. This
is the **server-side** WebAuthn/passkey implementation; the browser-side registration/login
ceremony JS lives in the separate [javascript/CLAUDE.md](../javascript/CLAUDE.md) module and is
synced into `spring-security-web` at build time, not into this module.

## Module

Gradle project `:spring-security-webauthn` (`spring-security-webauthn.gradle`). Base package is
`org.springframework.security.web.webauthn` — nested under `.web.`, not a standalone
`org.springframework.security.webauthn`. Depends on `spring-security-core` and
`spring-security-web` (`api`), plus a **hard `api` dependency on `com.webauthn4j:webauthn4j-core`**
— unlike its optional/test-only appearances elsewhere (`web`, `docs`), this module builds directly
on webauthn4j for the actual ceremony/attestation verification logic. `spring-jdbc`/`spring-tx`
and both Jackson generations are `optional`; `jakarta.servlet-api` is `provided`. Servlet-only —
no reactive code anywhere in `src/main/java` (reactor/webflux test dependencies exist only for
this module's own tests, not production code). Not related to `spring-security-data`; persistence
uses plain `spring-jdbc`, not Spring Data/JPA.

Test scoping: `./gradlew :spring-security-webauthn:test`.

## webauthn4j isolation — same pattern as Nimbus in `oauth2-core`

`com.webauthn4j.*` types are used in **exactly one file**: `management/Webauthn4JRelyingPartyOperations`.
Every other class in this module — including the entire `api` package — is Spring Security's own
type, never a webauthn4j type. This is a deliberate isolation boundary, following the same
pattern as Nimbus's single-class confinement in `oauth2-core` and OpenSAML's `OpenSamlOperations`
SPI in `saml2-service-provider` (though here there's no dual-version build — just one adapter
class). When extending ceremony behavior, keep new webauthn4j usage inside this one class; add a
method to `WebAuthnRelyingPartyOperations` (the interface) rather than reaching for webauthn4j
types from a filter or repository class.

## Package map

- `api/` — the WebAuthn/CTAP domain model, entirely Spring Security's own types (not webauthn4j's):
  `PublicKeyCredentialCreationOptions`/`PublicKeyCredentialRequestOptions`,
  `PublicKeyCredentialRpEntity`, `PublicKeyCredentialUserEntity`/`ImmutablePublicKeyCredentialUserEntity`,
  `PublicKeyCredentialParameters`/`Descriptor`, `CredentialRecord`/`ImmutableCredentialRecord` (the
  persisted credential — public key, sign count, transports, etc.),
  `AuthenticatorAttestationResponse`/`AuthenticatorAssertionResponse`, `Bytes` (base64url byte
  wrapper used throughout instead of raw `byte[]`), `PublicKeyCose`, `COSEAlgorithmIdentifier`, and
  the various WebAuthn enums (`AttestationConveyancePreference`, `AuthenticatorAttachment`,
  `AuthenticatorTransport`, `ResidentKeyRequirement`, `UserVerificationRequirement`). There is no
  `RelyingPartyRegistration`-style config object like `oauth2-client`/`saml2` have — relying-party
  configuration (RP ID, name, allowed origins) is passed directly as constructor parameters into
  `Webauthn4JRelyingPartyOperations`.
- `management/` — orchestration: `WebAuthnRelyingPartyOperations` (the 4-method interface —
  `createPublicKeyCredentialCreationOptions`, `registerCredential`,
  `createCredentialRequestOptions`, `authenticate`) implemented by
  `Webauthn4JRelyingPartyOperations` (the one class touching webauthn4j — see above); supporting
  request/response types `PublicKeyCredentialCreationOptionsRequest`/
  `PublicKeyCredentialRequestOptionsRequest`, `RelyingPartyRegistrationRequest`/
  `RelyingPartyAuthenticationRequest`, `RelyingPartyPublicKey`,
  `CredentialRecordOwnerAuthorizationManager` (ensures a user can only manage their own
  credentials).
- `registration/` — servlet endpoints for the registration ceremony, all `OncePerRequestFilter`s:
  `PublicKeyCredentialCreationOptionsFilter` (issues the challenge/options),
  `WebAuthnRegistrationFilter` (verifies the attestation and stores the credential),
  `DefaultWebAuthnRegistrationPageGeneratingFilter` (serves a default HTML registration page via
  `HtmlTemplates`). Per-request options are stashed via
  `PublicKeyCredentialCreationOptionsRepository`, default
  `HttpSessionPublicKeyCredentialCreationOptionsRepository`.
- `authentication/` — the login ceremony: `PublicKeyCredentialRequestOptionsFilter`
  (`OncePerRequestFilter`, issues the challenge), `WebAuthnAuthenticationFilter` (extends
  `AbstractAuthenticationProcessingFilter`, the actual login submission handler),
  `WebAuthnAuthenticationProvider`, `WebAuthnAuthenticationRequestToken`/`WebAuthnAuthentication`.
  Options repository follows the same pattern:
  `PublicKeyCredentialRequestOptionsRepository`/`HttpSessionPublicKeyCredentialRequestOptionsRepository`.
- Credential storage: `UserCredentialRepository` (`MapUserCredentialRepository` in-memory,
  `JdbcUserCredentialRepository` with SQL inlined as `private static final String ..._SQL`
  constants — no separate `.sql` schema file in this module) and its companion
  `PublicKeyCredentialUserEntityRepository` (`Map*`/`Jdbc*` implementations).
- `jackson/` — unusually extensive for this module: full Jackson 2 (`*Jackson2Mixin`/
  `*Jackson2Serializer`/`*Jackson2Deserializer`, `WebauthnJackson2Module`) and Jackson 3
  (`*Mixin`/`*Serializer`/`*Deserializer`, `WebauthnJacksonModule`) coverage for every `api` type —
  WebAuthn's binary/CBOR-flavored data (challenges, credential IDs, COSE keys) needs custom
  (de)serializers, not just mixins, unlike simpler modules' dual-Jackson packages.
- `aot/` — `PublicKeyCredentialUserEntityRuntimeHints`, `UserCredentialRuntimeHints`.
