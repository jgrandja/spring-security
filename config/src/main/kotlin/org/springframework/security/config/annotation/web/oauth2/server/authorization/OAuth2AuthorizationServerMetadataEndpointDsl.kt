/*
 * Copyright 2004-present the original author or authors.
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

package org.springframework.security.config.annotation.web.oauth2.server.authorization

import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerMetadataEndpointConfigurer
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata

/**
 * A Kotlin DSL to configure the OAuth 2.0 Authorization Server Metadata Endpoint using
 * idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2AuthorizationServerMetadataEndpointDsl {
    private var authorizationServerMetadataCustomizer: ((OAuth2AuthorizationServerMetadata.Builder) -> Unit)? = null

    /**
     * Sets the [Consumer] providing access to the
     * [OAuth2AuthorizationServerMetadata.Builder] allowing the ability to customize the
     * claims of the Authorization Server's configuration.
     *
     * @param authorizationServerMetadataCustomizer the [Consumer] providing access to
     * the [OAuth2AuthorizationServerMetadata.Builder]
     */
    fun authorizationServerMetadataCustomizer(authorizationServerMetadataCustomizer: (OAuth2AuthorizationServerMetadata.Builder) -> Unit) {
        this.authorizationServerMetadataCustomizer = authorizationServerMetadataCustomizer
    }

    internal fun get(): (OAuth2AuthorizationServerMetadataEndpointConfigurer) -> Unit {
        return { authorizationServerMetadataEndpoint ->
            authorizationServerMetadataCustomizer?.also {
                authorizationServerMetadataEndpoint.authorizationServerMetadataCustomizer(authorizationServerMetadataCustomizer)
            }
        }
    }
}
