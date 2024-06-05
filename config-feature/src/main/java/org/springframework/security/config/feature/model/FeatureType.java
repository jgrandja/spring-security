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

import org.springframework.util.Assert;

/**
 * @author Joe Grandja
 */
public final class FeatureType {

	public static final FeatureType AUTHENTICATION = new FeatureType("feature.type.authentication");

	public static final FeatureType AUTHORIZATION = new FeatureType("feature.type.authorization");

	public static final FeatureType EXPLOIT_PROTECTION = new FeatureType("feature.type.exploit-protection");

	private final String value;

	public FeatureType(String value) {
		Assert.hasText(value, "value cannot be empty");
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		FeatureType that = (FeatureType) obj;
		return getValue().equals(that.getValue());
	}

	@Override
	public int hashCode() {
		return getValue().hashCode();
	}

}
