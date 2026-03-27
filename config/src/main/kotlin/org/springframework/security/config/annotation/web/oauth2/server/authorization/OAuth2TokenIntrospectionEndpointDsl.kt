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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenIntrospectionEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Token Introspection Endpoint using idiomatic
 * Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property introspectionResponseHandler the [AuthenticationSuccessHandler] used for
 * handling a token introspection request and returning the Introspection Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * token introspection request error and returning the Error Response
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2TokenIntrospectionEndpointDsl {
    var introspectionResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var introspectionRequestConverter: AuthenticationConverter? = null
    private var introspectionRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract an Introspection
     * Request from the request.
     *
     * @param introspectionRequestConverter the [AuthenticationConverter] to add
     */
    fun introspectionRequestConverter(introspectionRequestConverter: AuthenticationConverter) {
        this.introspectionRequestConverter = introspectionRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param introspectionRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun introspectionRequestConverters(introspectionRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.introspectionRequestConvertersConsumer = introspectionRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a token introspection
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

    internal fun get(): (OAuth2TokenIntrospectionEndpointConfigurer) -> Unit {
        return { tokenIntrospectionEndpoint ->
            introspectionRequestConverter?.also { tokenIntrospectionEndpoint.introspectionRequestConverter(introspectionRequestConverter) }
            introspectionRequestConvertersConsumer?.also { tokenIntrospectionEndpoint.introspectionRequestConverters(introspectionRequestConvertersConsumer) }
            authenticationProvider?.also { tokenIntrospectionEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { tokenIntrospectionEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            introspectionResponseHandler?.also { tokenIntrospectionEndpoint.introspectionResponseHandler(introspectionResponseHandler) }
            errorResponseHandler?.also { tokenIntrospectionEndpoint.errorResponseHandler(errorResponseHandler) }
        }
    }
}
