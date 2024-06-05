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

import java.util.List;
import java.util.Map;

/**
 * @author Joe Grandja
 */
public final class UrlAuthorizationFeature extends Feature {

	public static final String FEATURE_ID = "feature.authorization.url";

	private UrlAuthorizationFeature(String id, FeatureType featureType, Map<String, Object> options) {
		super(id, featureType, options);
	}

	public List<String> getAllowed() {
		return getOption(Option.ALLOWED);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Option {

		public static final String ALLOWED = FEATURE_ID.concat(".option.allowed");

		private Option() {
		}

	}

	public static final class Builder extends Feature.Builder<UrlAuthorizationFeature, Builder> {

		private Builder() {
			super(FEATURE_ID, FeatureType.AUTHORIZATION);
		}

		public Builder allowed(List<String> allowed) {
			return option(Option.ALLOWED, allowed);
		}

		@Override
		public UrlAuthorizationFeature build() {
			return new UrlAuthorizationFeature(getId(), getFeatureType(), getOptions());
		}

	}

}
