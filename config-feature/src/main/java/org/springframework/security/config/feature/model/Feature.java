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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.util.Assert;

/**
 * @author Joe Grandja
 */
public abstract class Feature {

	private final String id;

	private final FeatureType featureType;

	private final Map<String, Object> options;

	protected Feature(String id, FeatureType featureType, Map<String, Object> options) {
		this.id = id;
		this.featureType = featureType;
		this.options = Map.copyOf(options);
	}

	public final String getId() {
		return this.id;
	}

	public final FeatureType getFeatureType() {
		return this.featureType;
	}

	public final Map<String, Object> getOptions() {
		return this.options;
	}

	@SuppressWarnings("unchecked")
	public final <T> T getOption(String name) {
		return (T) getOptions().get(name);
	}

	public abstract static class Builder<T extends Feature, B extends Builder<T, B>> {

		private final String id;

		private final FeatureType featureType;

		private final Map<String, Object> options = new HashMap<>();

		protected Builder(String id, FeatureType featureType) {
			Assert.hasText(id, "id cannot be empty");
			Assert.notNull(featureType, "featureType cannot be null");
			this.id = id;
			this.featureType = featureType;
		}

		protected final String getId() {
			return this.id;
		}

		protected final FeatureType getFeatureType() {
			return this.featureType;
		}

		protected final Map<String, Object> getOptions() {
			return this.options;
		}

		public final B option(String name, Object value) {
			Assert.hasText(name, "name cannot be empty");
			Assert.notNull(value, "value cannot be null");
			getOptions().put(name, value);
			return getThis();
		}

		public final B options(Consumer<Map<String, Object>> optionsConsumer) {
			optionsConsumer.accept(getOptions());
			return getThis();
		}

		@SuppressWarnings("unchecked")
		protected final B getThis() {
			return (B) this;
		}

		public abstract T build();

	}

}
