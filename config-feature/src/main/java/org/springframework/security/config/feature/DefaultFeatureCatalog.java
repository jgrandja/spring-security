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

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.security.config.feature.model.Feature;
import org.springframework.security.config.feature.model.FeatureType;
import org.springframework.util.Assert;

/**
 * @author Joe Grandja
 */
public final class DefaultFeatureCatalog implements FeatureCatalog {

	private final List<Feature> features = new ArrayList<>();

	public void register(Feature feature) {
		Assert.notNull(feature, "feature cannot be null");
		this.features.add(feature);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T extends Feature> T getById(String id) {
		Assert.hasText(id, "id cannot be empty");
		for (Feature feature : this.features) {
			if (feature.getId().equals(id)) {
				return (T) feature;
			}
		}
		return null;
	}

	@Override
	public List<Feature> getAll() {
		return List.copyOf(this.features);
	}

	@Override
	public List<Feature> filterByType(FeatureType featureType) {
		Assert.notNull(featureType, "featureType cannot be null");
		List<Feature> features = new ArrayList<>();
		for (Feature feature : this.features) {
			if (feature.getFeatureType().equals(featureType)) {
				features.add(feature);
			}
		}
		return features;
	}

	@Override
	public List<Feature> filterByTag(String tag) {
		return null;
	}

}
