/*
 * Copyright 2002-2024 the original author or authors.
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

package sample.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.feature.Features;
import org.springframework.security.config.feature.configure.Configurable;
import org.springframework.security.config.feature.configure.DefaultFeatureConfigurationContext;
import org.springframework.security.config.feature.configure.FeatureConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Import(FeatureConfiguration.class)
@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Value("${jwt.public.key}")
	private RSAPublicKey publicKey;

	@Value("${jwt.private.key}")
	private RSAPrivateKey privateKey;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
			Features features, List<FeatureConfigurer> featureConfigurers) throws Exception {

		Configurable<HttpSecurity> configurable = () -> httpSecurity;

		features.toList().forEach((feature) ->
			featureConfigurers.forEach((featureConfigurer) ->
				featureConfigurer.configure(
						new DefaultFeatureConfigurationContext(feature, configurable)
				)
			)
		);

		return httpSecurity.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

	@Bean
	public UserDetailsService userDetailsService() {
		// @formatter:off
		UserDetails user = User.withDefaultPasswordEncoder()
				.username("user")
				.password("password")
				.authorities("app")
				.build();
		// @formatter:on
		return new InMemoryUserDetailsManager(user);
	}

}
