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

package org.springframework.security.config.feature.configure;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.feature.model.OAuth2ResourceServerFeature;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;

/**
 * @author Joe Grandja
 */
public final class OAuth2ResourceServerFeatureConfigurer implements FeatureConfigurer {

	@Override
	public void configure(FeatureConfigurationContext configurationContext) {
		if (!configurationContext.getFeature().getId().equals(OAuth2ResourceServerFeature.FEATURE_ID)) {
			return;
		}

		OAuth2ResourceServerFeature oauth2ResourceServerFeature = configurationContext.getFeature();
		HttpSecurity httpSecurity = configurationContext.<HttpSecurity>getConfigurable().getSource();

		try {
			// @formatter:off
			httpSecurity
				.oauth2ResourceServer((oauth2ResourceServer) ->
					oauth2ResourceServer.jwt(Customizer.withDefaults()))
				.sessionManagement((session) ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling((exceptions) -> exceptions
					.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
					.accessDeniedHandler(new BearerTokenAccessDeniedHandler())
				);
			// @formatter:on
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
