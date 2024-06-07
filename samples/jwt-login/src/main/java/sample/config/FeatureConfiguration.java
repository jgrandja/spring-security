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

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.feature.DefaultFeatureCatalog;
import org.springframework.security.config.feature.FeatureCatalog;
import org.springframework.security.config.feature.Features;
import org.springframework.security.config.feature.configure.CsrfExploitProtectionFeatureConfigurer;
import org.springframework.security.config.feature.configure.FeatureConfigurer;
import org.springframework.security.config.feature.configure.HttpBasicAuthenticationFeatureConfigurer;
import org.springframework.security.config.feature.configure.OAuth2ResourceServerFeatureConfigurer;
import org.springframework.security.config.feature.configure.UrlAuthorizationFeatureConfigurer;
import org.springframework.security.config.feature.model.CsrfExploitProtectionFeature;
import org.springframework.security.config.feature.model.HttpBasicAuthenticationFeature;
import org.springframework.security.config.feature.model.OAuth2ResourceServerFeature;
import org.springframework.security.config.feature.model.UrlAuthorizationFeature;

@Configuration(proxyBeanMethods = false)
public class FeatureConfiguration {

	@Bean
	public FeatureCatalog featureCatalog() {
		DefaultFeatureCatalog featureCatalog = new DefaultFeatureCatalog();
		featureCatalog.register(HttpBasicAuthenticationFeature.builder().build());
		featureCatalog.register(CsrfExploitProtectionFeature.builder().build());
		featureCatalog.register(UrlAuthorizationFeature.builder().build());
		featureCatalog.register(OAuth2ResourceServerFeature.builder().build());
		return featureCatalog;
	}

	@Bean
	public List<FeatureConfigurer> featureConfigurers() {
		return Arrays.asList(
				new HttpBasicAuthenticationFeatureConfigurer(),
				new CsrfExploitProtectionFeatureConfigurer(),
				new UrlAuthorizationFeatureConfigurer(),
				new OAuth2ResourceServerFeatureConfigurer());
	}

	@Bean
	public Features features(FeatureCatalog featureCatalog) {
		// @formatter:off
		return Features.using(featureCatalog)
				.add(HttpBasicAuthenticationFeature.class)
				.customize(CsrfExploitProtectionFeature.Builder.class,
					(builder) -> builder
						.ignored(List.of("/token"))
				)
				.add(UrlAuthorizationFeature.class)
				.add(OAuth2ResourceServerFeature.class);
		// @formatter:on
	}

}
