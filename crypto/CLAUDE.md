# CLAUDE.md — spring-security-crypto

Guidance specific to this module. See the root [CLAUDE.md](../CLAUDE.md) for repo-wide build,
formatting, and contribution conventions — this file only adds what's local to `crypto`.

## Module

Gradle project `:spring-security-crypto` (`spring-security-crypto.gradle`). Lowest-level module in
the dependency graph — no dependency on any other `spring-security-*` module. `spring-core` is
`optional`; BouncyCastle (`org.bouncycastle:bcpkix-jdk18on`) and Password4j
(`com.password4j:password4j`, via `libs.com.password4j.password4j`) are also `optional` —
implementations that depend on them must degrade or fail clearly at runtime if the library isn't
on the classpath, not at compile time. Uses `java-test-fixtures`, so shared test helpers live under
`src/testFixtures`, not just `src/test`.

Test scoping: `./gradlew :spring-security-crypto:test`, or with a filter:
`./gradlew :spring-security-crypto:test --tests "*BCryptPasswordEncoder*"`.

## Package map

- `password/` — the `PasswordEncoder` interface and JDK-native implementations:
  `BCryptPasswordEncoder`-adjacent digest encoders (`Md4PasswordEncoder`,
  `MessageDigestPasswordEncoder`, `LdapShaPasswordEncoder`, `StandardPasswordEncoder`,
  `Pbkdf2PasswordEncoder`, `NoOpPasswordEncoder`), and `DelegatingPasswordEncoder`, which picks an
  encoder by a `{id}` prefix stored with the hash.
- `bcrypt/`, `scrypt/`, `argon2/` — one JDK-native `PasswordEncoder` implementation each, named
  for the algorithm.
- `password4j/` — a **second, parallel set** of `PasswordEncoder` implementations for the same
  algorithms (`BcryptPassword4jPasswordEncoder`, `ScryptPassword4jPasswordEncoder`,
  `Argon2Password4jPasswordEncoder`, `Pbkdf2Password4jPasswordEncoder`,
  `BalloonHashingPassword4jPasswordEncoder`), backed by the optional Password4j library instead of
  this module's own implementation. All extend the package-private abstract
  `Password4jPasswordEncoder` (itself extending `AbstractValidatingPasswordEncoder` from
  `password/`). Adding a new algorithm to `bcrypt`/`scrypt`/`argon2` does not automatically imply a
  `password4j` counterpart is expected — check whether Password4j actually supports it first.
- `factory/PasswordEncoderFactories` — wires together the default `DelegatingPasswordEncoder`
  (`createDelegatingPasswordEncoder()`), mapping `{id}` prefixes to concrete encoders from
  `password/`, `bcrypt/`, `scrypt/`, `argon2/` (not `password4j/`). Each encoder generally exposes
  versioned factory methods (e.g. `Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()`,
  `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_2()`) — new versions add a new method rather
  than changing defaults of an existing one, since `DelegatingPasswordEncoder` persists the id with
  the hash and must keep decoding old hashes correctly.
- `encrypt/` — symmetric/asymmetric byte and text encryption: `Encryptors` (factory),
  `BytesEncryptor`/`TextEncryptor`, AES-CBC/AES-GCM implementations in both JDK-native
  (`AesBytesEncryptor`, `AesCbcBytesEncryptor`, `AesGcmBytesEncryptor`) and BouncyCastle-backed
  (`BouncyCastleAesBytesEncryptor`, `BouncyCastleAesCbcBytesEncryptor`,
  `BouncyCastleAesGcmBytesEncryptor`) variants, plus RSA helpers (`RsaKeyHelper`, `RsaRawEncryptor`,
  `RsaSecretEncryptor`) and `KeyStoreKeyFactory`.
- `keygen/` — `BytesKeyGenerator`/`StringKeyGenerator` implementations and the `KeyGenerators`
  factory (hex/Base64-encoded, secure-random).
- `codec/` — small standalone helpers (`Hex`, `Utf8`) with no framework dependency.
- `util/EncodingUtils` — low-level byte array helpers (concatenate/subarray) used by the `encrypt`
  and `password` implementations.

## Conventions

- Every `PasswordEncoder`/`BytesEncryptor` change must consider backward compatibility of
  previously-encoded values — this module has no knowledge of stored data, so behavior changes to
  an existing encoder's `encode`/`matches` are effectively permanent; prefer adding a new versioned
  factory method (see `factory/PasswordEncoderFactories`) over changing one in place.
- Runtime-optional dependencies (BouncyCastle, Password4j) must not be referenced from code paths
  that are reachable without the user opting into that specific encoder/encryptor — keep them
  isolated to their own classes so the rest of the module works with only `spring-core` on the
  classpath.
