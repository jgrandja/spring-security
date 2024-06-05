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

package org.springframework.security.config.feature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.security.config.feature.model.Feature;
import org.springframework.util.Assert;

/**
 * @author Joe Grandja
 */
final class FeaturesImpl implements Features {

	private final FeatureCatalog featureCatalog;

	private final List<Feature> features = new ArrayList<>();

	FeaturesImpl(FeatureCatalog featureCatalog) {
		Assert.notNull(featureCatalog, "featureCatalog cannot be null");
		this.featureCatalog = featureCatalog;
	}

	@Override
	public <T extends Feature> Features add(Class<T> featureType) {
		Assert.notNull(featureType, "featureType cannot be null");
		T feature = findFeature(featureType);
		if (feature != null) {
			Feature.Builder<T, ?> builder = createBuilder(feature);
			this.features.add(builder.build());
		}
		return this;
	}

	@Override
	public <T extends Feature> Features addWithTag(String tag) {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Feature, B extends Feature.Builder<T, B>> Features customize(Class<B> builderType,
			Consumer<B> customizer) {
		Assert.notNull(builderType, "builderType cannot be null");
		Assert.notNull(customizer, "customizer cannot be null");
		Class<T> featureType = (Class<T>) builderType.getDeclaringClass();
		T feature = findFeature(featureType);
		if (feature != null) {
			B builder = (B) createBuilder(feature);
			customizer.accept(builder);
			this.features.add(builder.build());
		}
		return this;
	}

	@Override
	public List<Feature> toList() {
		return new ArrayList<>(this.features);
	}

	@SuppressWarnings("unchecked")
	private <T extends Feature> T findFeature(Class<T> featureType) {
		for (Feature feature : this.featureCatalog.getAll()) {
			if (feature.getClass().isAssignableFrom(featureType)) {
				return (T) feature;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T extends Feature> Feature.Builder<T, ?> createBuilder(T feature) {
		try {
			Method builderMethod = feature.getClass().getDeclaredMethod("builder");
			return (Feature.Builder<T, ?>) builderMethod.invoke(feature);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
