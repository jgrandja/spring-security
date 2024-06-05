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

package org.springframework.security.config.feature.model;

import java.util.Map;

/**
 * @author Joe Grandja
 */
public final class FormAuthenticationFeature extends Feature {

	public static final String FEATURE_ID = "feature.authentication.form";

	private FormAuthenticationFeature(String id, FeatureType featureType, Map<String, Object> options) {
		super(id, featureType, options);
	}

	public String getLoginPage() {
		return getOption(Option.LOGIN_PAGE);
	}

	public String getDefaultSuccessUrl() {
		return getOption(Option.DEFAULT_SUCCESS_URL);
	}

	public boolean isDefaultSuccessUrlAlwaysUse() {
		return getOption(Option.DEFAULT_SUCCESS_URL_ALWAYS_USE);
	}

	public String getFailureUrl() {
		return getOption(Option.FAILURE_URL);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Option {

		public static final String LOGIN_PAGE = FEATURE_ID.concat(".option.login-page");

		public static final String DEFAULT_SUCCESS_URL = FEATURE_ID.concat(".option.default-success-url");

		public static final String DEFAULT_SUCCESS_URL_ALWAYS_USE = FEATURE_ID
			.concat(".option.default-success-url-always-use");

		public static final String FAILURE_URL = FEATURE_ID.concat(".option.failure-url");

		private Option() {
		}

	}

	public static final class Builder extends Feature.Builder<FormAuthenticationFeature, Builder> {

		private Builder() {
			super(FEATURE_ID, FeatureType.AUTHENTICATION);
		}

		public Builder loginPage(String loginPage) {
			return option(Option.LOGIN_PAGE, loginPage);
		}

		public Builder defaultSuccessUrl(String defaultSuccessUrl) {
			return option(Option.DEFAULT_SUCCESS_URL, defaultSuccessUrl);
		}

		public Builder defaultSuccessUrlAlwaysUse(boolean defaultSuccessUrlAlwaysUse) {
			return option(Option.DEFAULT_SUCCESS_URL_ALWAYS_USE, defaultSuccessUrlAlwaysUse);
		}

		public Builder failureUrl(String failureUrl) {
			return option(Option.FAILURE_URL, failureUrl);
		}

		@Override
		public FormAuthenticationFeature build() {
			return new FormAuthenticationFeature(getId(), getFeatureType(), getOptions());
		}

	}

}
