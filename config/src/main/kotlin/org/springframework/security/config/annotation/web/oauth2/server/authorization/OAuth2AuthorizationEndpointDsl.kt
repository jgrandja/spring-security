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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Authorization Endpoint using idiomatic Kotlin
 * code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property authorizationResponseHandler the [AuthenticationSuccessHandler] used for
 * handling an authorization code request and returning the Authorization Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling an
 * authorization request error and returning the Error Response
 * @property consentPage the URI of the custom consent page to redirect to if consent is
 * required (e.g. "/oauth2/consent")
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2AuthorizationEndpointDsl {
    var authorizationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null
    var consentPage: String? = null

    private var authorizationRequestConverter: AuthenticationConverter? = null
    private var authorizationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract an Authorization
     * Request from the request.
     *
     * @param authorizationRequestConverter the [AuthenticationConverter] to add
     */
    fun authorizationRequestConverter(authorizationRequestConverter: AuthenticationConverter) {
        this.authorizationRequestConverter = authorizationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param authorizationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun authorizationRequestConverters(authorizationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.authorizationRequestConvertersConsumer = authorizationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating an authorization code
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

    internal fun get(): (OAuth2AuthorizationEndpointConfigurer) -> Unit {
        return { authorizationEndpoint ->
            authorizationRequestConverter?.also { authorizationEndpoint.authorizationRequestConverter(authorizationRequestConverter) }
            authorizationRequestConvertersConsumer?.also { authorizationEndpoint.authorizationRequestConverters(authorizationRequestConvertersConsumer) }
            authenticationProvider?.also { authorizationEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { authorizationEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            authorizationResponseHandler?.also { authorizationEndpoint.authorizationResponseHandler(authorizationResponseHandler) }
            errorResponseHandler?.also { authorizationEndpoint.errorResponseHandler(errorResponseHandler) }
            consentPage?.also { authorizationEndpoint.consentPage(consentPage) }
        }
    }
}
