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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcLogoutEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OpenID Connect 1.0 RP-Initiated Logout Endpoint using
 * idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property logoutResponseHandler the [AuthenticationSuccessHandler] used for handling a
 * logout request and performing the logout
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * logout request error and returning the Error Response
 */
@OAuth2AuthorizationServerSecurityMarker
class OidcLogoutEndpointDsl {
    var logoutResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var logoutRequestConverter: AuthenticationConverter? = null
    private var logoutRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Logout Request
     * from the request.
     *
     * @param logoutRequestConverter the [AuthenticationConverter] to add
     */
    fun logoutRequestConverter(logoutRequestConverter: AuthenticationConverter) {
        this.logoutRequestConverter = logoutRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param logoutRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun logoutRequestConverters(logoutRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.logoutRequestConvertersConsumer = logoutRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a logout request.
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

    internal fun get(): (OidcLogoutEndpointConfigurer) -> Unit {
        return { logoutEndpoint ->
            logoutRequestConverter?.also { logoutEndpoint.logoutRequestConverter(logoutRequestConverter) }
            logoutRequestConvertersConsumer?.also { logoutEndpoint.logoutRequestConverters(logoutRequestConvertersConsumer) }
            authenticationProvider?.also { logoutEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { logoutEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            logoutResponseHandler?.also { logoutEndpoint.logoutResponseHandler(logoutResponseHandler) }
            errorResponseHandler?.also { logoutEndpoint.errorResponseHandler(errorResponseHandler) }
        }
    }
}
