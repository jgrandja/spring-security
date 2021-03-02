/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.oauth2.jwt;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class NimbusJwtBuilderFactory<C extends SecurityContext> implements JwtBuilderFactory {
	private JWKSource<C> jwksSource;

	public void setJwkSource(JWKSource<C> jwksSource) {
		Assert.notNull(jwksSource, "jwkSelector cannot be null");
		this.jwksSource = jwksSource;
	}

	@Override
	public NimbusJwtBuilder<C> create() {
		// JG:
		// I realize this is an implementation detail but this.jwksSource could be null
		return new NimbusJwtBuilder<>(this.jwksSource)
				.joseHeader((headers) -> headers.algorithm(SignatureAlgorithm.RS256).type("JWT"))
				.claimsSet((claims) -> claims.id(UUID.randomUUID().toString()));
	}

	// JG:
	// 1) This implementation of JwtBuilder is NOT thread-safe.
	// 	The headers and claims in super (JwtBuilderSupport) can only be used on a per-request basis.
	// 	For example, after the first caller customizes the headers and claims, the 2nd caller will
	// 	start off where the first caller left off, and so on...
	//	NOTE: The JwtEncoder API promotes thread-safety given that the per-request data
	//	is supplied via it's input arguments JoseHeader and JwtClaimsSet.
	// 2) This is a "heavy-weight" object that will be costly to garbage collect
	// 	given the members jwsSigners, jwkSource, jwk, context.
	// 3) It's not clear to me where the source key comes from?
	// 	There are 3 potential sources: jwkSource, jwk, context. Too many choices could lead to confusion.
	public static final class NimbusJwtBuilder<C extends SecurityContext> extends JwtBuilderSupport<NimbusJwtBuilder<C>> implements JwtBuilder<NimbusJwtBuilder<C>> {
		private static final String ENCODING_ERROR_MESSAGE_TEMPLATE = "An error occurred while attempting to encode the Jwt: %s";

		private static final Converter<JoseHeader, JWSHeader> JWS_HEADER_CONVERTER = new JwsHeaderConverter();

		private static final Converter<JwtClaimsSet, JWTClaimsSet> JWT_CLAIMS_SET_CONVERTER = new JwtClaimsSetConverter();

		private static final JWSSignerFactory JWS_SIGNER_FACTORY = new DefaultJWSSignerFactory();

		private final Map<JWK, JWSSigner> jwsSigners = new ConcurrentHashMap<>();

		private final JWKSource<C> jwkSource;

		private JWK jwk;
		private C context;

		private NimbusJwtBuilder(JWKSource<C> jwkSource) {
			this.jwkSource = jwkSource;
		}

		// JG:
		// I could set JWKSource in the factory and also set an individual JWK in this builder.
		// Why not keep it consistent and only allow for JWKSource?
		// Another implementation detail but this would simplify from a user perspective.
		public NimbusJwtBuilder<C> jwk(JWK jwk) {
			this.jwk = jwk;
			if (this.jwk.getKeyID() != null) {
				this.headers.keyId(this.jwk.getKeyID());
			}
			if (this.jwk.getX509CertSHA256Thumbprint() != null) {
				this.headers.x509SHA256Thumbprint(this.jwk.getX509CertSHA256Thumbprint().toString());
			}
			return this;
		}

		public NimbusJwtBuilder<C> securityContext(C context) {
			this.context = context;
			return this;
		}

		@Override
		public Jwt encode() {
			if (this.jwk == null) {
				this.headers.headers((header) -> jwk(selectJwk(header)));
			}

			JoseHeader header = this.headers.build();
			JwtClaimsSet claims = this.claims.build();

			JWSHeader jwsHeader = JWS_HEADER_CONVERTER.convert(header);
			JWTClaimsSet jwtClaimsSet = JWT_CLAIMS_SET_CONVERTER.convert(claims);

			JWSSigner jwsSigner = this.jwsSigners.computeIfAbsent(this.jwk, (key) -> {
				try {
					return JWS_SIGNER_FACTORY.createJWSSigner(key);
				}
				catch (JOSEException ex) {
					throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
							"Failed to create a JWS Signer -> " + ex.getMessage()), ex);
				}
			});

			SignedJWT signedJwt = new SignedJWT(jwsHeader, jwtClaimsSet);
			try {
				signedJwt.sign(jwsSigner);
			}
			catch (JOSEException ex) {
				throw new JwtEncodingException(
						String.format(ENCODING_ERROR_MESSAGE_TEMPLATE, "Failed to sign the JWT -> " + ex.getMessage()), ex);
			}
			String jws = signedJwt.serialize();

			return new Jwt(jws, claims.getIssuedAt(), claims.getExpiresAt(), header.getHeaders(), claims.getClaims());
		}

		private JWK selectJwk(Map<String, Object> headers) {
			Map<String, Object> keySelectionHeaders = new HashMap<>(headers);
			String algorithm = headers.get(JoseHeaderNames.ALG).toString();
			keySelectionHeaders.put(JoseHeaderNames.ALG, algorithm);
			JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(algorithm);

			List<JWK> jwks;
			try {
				JWSHeader jwsHeader = JWSHeader.parse(keySelectionHeaders);
				JWKSelector jwkSelector = new JWKSelector(JWKMatcher.forJWSHeader(jwsHeader));
				jwks = this.jwkSource.get(jwkSelector, this.context);
			}
			catch (Exception ex) {
				throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
						"Failed to select a JWK signing key -> " + ex.getMessage()), ex);
			}

			if (jwks.size() > 1) {
				throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
						"Found multiple JWK signing keys for algorithm '" + jwsAlgorithm.getName() + "'"));
			}

			if (jwks.isEmpty()) {
				throw new JwtEncodingException(
						String.format(ENCODING_ERROR_MESSAGE_TEMPLATE, "Failed to select a JWK signing key"));
			}

			return jwks.get(0);
		}

		private static class JwsHeaderConverter implements Converter<JoseHeader, JWSHeader> {

			@Override
			public JWSHeader convert(JoseHeader headers) {
				JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.parse(headers.getAlgorithm().getName()));

				Set<String> critical = headers.getCritical();
				if (!CollectionUtils.isEmpty(critical)) {
					builder.criticalParams(critical);
				}

				String contentType = headers.getContentType();
				if (StringUtils.hasText(contentType)) {
					builder.contentType(contentType);
				}

				URL jwkSetUri = headers.getJwkSetUri();
				if (jwkSetUri != null) {
					try {
						builder.jwkURL(jwkSetUri.toURI());
					}
					catch (Exception ex) {
						throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
								"Failed to convert '" + JoseHeaderNames.JKU + "' JOSE header to a URI"), ex);
					}
				}

				Map<String, Object> jwk = headers.getJwk();
				if (!CollectionUtils.isEmpty(jwk)) {
					try {
						builder.jwk(JWK.parse(jwk));
					}
					catch (Exception ex) {
						throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
								"Failed to convert '" + JoseHeaderNames.JWK + "' JOSE header"), ex);
					}
				}

				String keyId = headers.getKeyId();
				if (StringUtils.hasText(keyId)) {
					builder.keyID(keyId);
				}

				String type = headers.getType();
				if (StringUtils.hasText(type)) {
					builder.type(new JOSEObjectType(type));
				}

				List<String> x509CertificateChain = headers.getX509CertificateChain();
				if (!CollectionUtils.isEmpty(x509CertificateChain)) {
					builder.x509CertChain(x509CertificateChain.stream().map(Base64::new).collect(Collectors.toList()));
				}

				String x509SHA1Thumbprint = headers.getX509SHA1Thumbprint();
				if (StringUtils.hasText(x509SHA1Thumbprint)) {
					builder.x509CertThumbprint(new Base64URL(x509SHA1Thumbprint));
				}

				String x509SHA256Thumbprint = headers.getX509SHA256Thumbprint();
				if (StringUtils.hasText(x509SHA256Thumbprint)) {
					builder.x509CertSHA256Thumbprint(new Base64URL(x509SHA256Thumbprint));
				}

				URL x509Uri = headers.getX509Uri();
				if (x509Uri != null) {
					try {
						builder.x509CertURL(x509Uri.toURI());
					}
					catch (Exception ex) {
						throw new JwtEncodingException(String.format(ENCODING_ERROR_MESSAGE_TEMPLATE,
								"Failed to convert '" + JoseHeaderNames.X5U + "' JOSE header to a URI"), ex);
					}
				}

				Map<String, Object> customHeaders = headers.getHeaders().entrySet().stream()
						.filter((header) -> !JWSHeader.getRegisteredParameterNames().contains(header.getKey()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
				if (!CollectionUtils.isEmpty(customHeaders)) {
					builder.customParams(customHeaders);
				}

				return builder.build();
			}

		}

		private static class JwtClaimsSetConverter implements Converter<JwtClaimsSet, JWTClaimsSet> {

			@Override
			public JWTClaimsSet convert(JwtClaimsSet claims) {
				JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

				URL issuer = claims.getIssuer();
				if (issuer != null) {
					builder.issuer(issuer.toExternalForm());
				}

				String subject = claims.getSubject();
				if (StringUtils.hasText(subject)) {
					builder.subject(subject);
				}

				List<String> audience = claims.getAudience();
				if (!CollectionUtils.isEmpty(audience)) {
					builder.audience(audience);
				}

				Instant issuedAt = claims.getIssuedAt();
				if (issuedAt != null) {
					builder.issueTime(Date.from(issuedAt));
				}

				Instant expiresAt = claims.getExpiresAt();
				if (expiresAt != null) {
					builder.expirationTime(Date.from(expiresAt));
				}

				Instant notBefore = claims.getNotBefore();
				if (notBefore != null) {
					builder.notBeforeTime(Date.from(notBefore));
				}

				String jwtId = claims.getId();
				if (StringUtils.hasText(jwtId)) {
					builder.jwtID(jwtId);
				}

				Map<String, Object> customClaims = claims.getClaims().entrySet().stream()
						.filter((claim) -> !JWTClaimsSet.getRegisteredNames().contains(claim.getKey()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
				if (!CollectionUtils.isEmpty(customClaims)) {
					customClaims.forEach(builder::claim);
				}

				return builder.build();
			}

		}
	}
}
