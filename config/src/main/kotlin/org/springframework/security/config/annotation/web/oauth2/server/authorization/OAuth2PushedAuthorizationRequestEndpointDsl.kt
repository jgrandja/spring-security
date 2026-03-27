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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2PushedAuthorizationRequestEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Pushed Authorization Request Endpoint using
 * idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property pushedAuthorizationResponseHandler the [AuthenticationSuccessHandler] used
 * for handling a pushed authorization request and returning the Pushed Authorization
 * Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * pushed authorization request error and returning the Error Response
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2PushedAuthorizationRequestEndpointDsl {
    var pushedAuthorizationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var pushedAuthorizationRequestConverter: AuthenticationConverter? = null
    private var pushedAuthorizationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Pushed
     * Authorization Request from the request.
     *
     * @param pushedAuthorizationRequestConverter the [AuthenticationConverter] to add
     */
    fun pushedAuthorizationRequestConverter(pushedAuthorizationRequestConverter: AuthenticationConverter) {
        this.pushedAuthorizationRequestConverter = pushedAuthorizationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param pushedAuthorizationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun pushedAuthorizationRequestConverters(pushedAuthorizationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.pushedAuthorizationRequestConvertersConsumer = pushedAuthorizationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a pushed authorization
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

    internal fun get(): (OAuth2PushedAuthorizationRequestEndpointConfigurer) -> Unit {
        return { pushedAuthorizationRequestEndpoint ->
            pushedAuthorizationRequestConverter?.also { pushedAuthorizationRequestEndpoint.pushedAuthorizationRequestConverter(pushedAuthorizationRequestConverter) }
            pushedAuthorizationRequestConvertersConsumer?.also { pushedAuthorizationRequestEndpoint.pushedAuthorizationRequestConverters(pushedAuthorizationRequestConvertersConsumer) }
            authenticationProvider?.also { pushedAuthorizationRequestEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { pushedAuthorizationRequestEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            pushedAuthorizationResponseHandler?.also { pushedAuthorizationRequestEndpoint.pushedAuthorizationResponseHandler(pushedAuthorizationResponseHandler) }
            errorResponseHandler?.also { pushedAuthorizationRequestEndpoint.errorResponseHandler(errorResponseHandler) }
        }
    }
}
