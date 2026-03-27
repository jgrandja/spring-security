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

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenRevocationEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Token Revocation Endpoint using idiomatic
 * Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property revocationResponseHandler the [AuthenticationSuccessHandler] used for
 * handling a token revocation request and returning the Revocation Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * token revocation request error and returning the Error Response
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2TokenRevocationEndpointDsl {
    var revocationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var revocationRequestConverter: AuthenticationConverter? = null
    private var revocationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Revoke Token
     * Request from the request.
     *
     * @param revocationRequestConverter the [AuthenticationConverter] to add
     */
    fun revocationRequestConverter(revocationRequestConverter: AuthenticationConverter) {
        this.revocationRequestConverter = revocationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param revocationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun revocationRequestConverters(revocationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.revocationRequestConvertersConsumer = revocationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a token revocation
     * request.
     *
     * @param authenticationProvider the [AuthenticationProvider] to add
     */
    fun authenticationProvider(authenticationProvider: AuthenticationProvider) {
        this.authenticationProvider = authenticationProvider
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationProvider]'s.
     *
     * @param authenticationProvidersConsumer the [Consumer] for the list of providers
     */
    fun authenticationProviders(authenticationProvidersConsumer: (MutableList<AuthenticationProvider>) -> Unit) {
        this.authenticationProvidersConsumer = authenticationProvidersConsumer
    }

    internal fun get(): (OAuth2TokenRevocationEndpointConfigurer) -> Unit {
        return { tokenRevocationEndpoint ->
            revocationRequestConverter?.also { tokenRevocationEndpoint.revocationRequestConverter(revocationRequestConverter) }
            revocationRequestConvertersConsumer?.also { tokenRevocationEndpoint.revocationRequestConverters(revocationRequestConvertersConsumer) }
            authenticationProvider?.also { tokenRevocationEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { tokenRevocationEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            revocationResponseHandler?.also { tokenRevocationEndpoint.revocationResponseHandler(revocationResponseHandler) }
            errorResponseHandler?.also { tokenRevocationEndpoint.errorResponseHandler(errorResponseHandler) }
        }
    }
}
