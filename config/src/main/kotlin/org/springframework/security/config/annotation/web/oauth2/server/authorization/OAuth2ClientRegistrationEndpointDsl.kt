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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2ClientRegistrationEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Dynamic Client Registration Endpoint using
 * idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property clientRegistrationResponseHandler the [AuthenticationSuccessHandler] used
 * for handling a client registration request and returning the Client Registration
 * Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * client registration request error and returning the Error Response
 * @property openRegistrationAllowed whether open client registration (with no initial
 * access token) is allowed. The default is {@code false}.
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2ClientRegistrationEndpointDsl {
    var clientRegistrationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null
    var openRegistrationAllowed: Boolean? = null

    private var clientRegistrationRequestConverter: AuthenticationConverter? = null
    private var clientRegistrationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Client
     * Registration Request from the request.
     *
     * @param clientRegistrationRequestConverter the [AuthenticationConverter] to add
     */
    fun clientRegistrationRequestConverter(clientRegistrationRequestConverter: AuthenticationConverter) {
        this.clientRegistrationRequestConverter = clientRegistrationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param clientRegistrationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun clientRegistrationRequestConverters(clientRegistrationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.clientRegistrationRequestConvertersConsumer = clientRegistrationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a client registration
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

    internal fun get(): (OAuth2ClientRegistrationEndpointConfigurer) -> Unit {
        return { clientRegistrationEndpoint ->
            clientRegistrationRequestConverter?.also { clientRegistrationEndpoint.clientRegistrationRequestConverter(clientRegistrationRequestConverter) }
            clientRegistrationRequestConvertersConsumer?.also { clientRegistrationEndpoint.clientRegistrationRequestConverters(clientRegistrationRequestConvertersConsumer) }
            authenticationProvider?.also { clientRegistrationEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { clientRegistrationEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            clientRegistrationResponseHandler?.also { clientRegistrationEndpoint.clientRegistrationResponseHandler(clientRegistrationResponseHandler) }
            errorResponseHandler?.also { clientRegistrationEndpoint.errorResponseHandler(errorResponseHandler) }
            openRegistrationAllowed?.also { clientRegistrationEndpoint.openRegistrationAllowed(openRegistrationAllowed!!) }
        }
    }
}
