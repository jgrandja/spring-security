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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.feature.model.UrlAuthorizationFeature;
import org.springframework.util.CollectionUtils;

/**
 * @author Joe Grandja
 */
public final class UrlAuthorizationFeatureConfigurer implements FeatureConfigurer {

	@Override
	public void configure(FeatureConfigurationContext configurationContext) {
		if (!configurationContext.getFeature().getId().equals(UrlAuthorizationFeature.FEATURE_ID)) {
			return;
		}

		UrlAuthorizationFeature urlAuthorizationFeature = configurationContext.getFeature();
		HttpSecurity httpSecurity = configurationContext.<HttpSecurity>getConfigurable().getSource();

		try {
			// @formatter:off
			httpSecurity.authorizeHttpRequests((authorize) -> {
				if (!CollectionUtils.isEmpty(urlAuthorizationFeature.getAllowed())) {
					authorize.requestMatchers(urlAuthorizationFeature.getAllowed().toArray(new String[0])).permitAll();
				}
				authorize.anyRequest().authenticated();
			});
			// @formatter:on
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
