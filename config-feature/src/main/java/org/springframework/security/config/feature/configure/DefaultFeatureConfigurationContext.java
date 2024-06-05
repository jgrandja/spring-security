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

import org.springframework.security.config.feature.model.Feature;

/**
 * @author Joe Grandja
 */
public final class DefaultFeatureConfigurationContext implements FeatureConfigurationContext {

	private final Feature feature;

	private final Configurable<?> configurable;

	public DefaultFeatureConfigurationContext(Feature feature, Configurable<?> configurable) {
		this.feature = feature;
		this.configurable = configurable;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Feature> T getFeature() {
		return (T) this.feature;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> Configurable<C> getConfigurable() {
		return (Configurable<C>) this.configurable;
	}

}
