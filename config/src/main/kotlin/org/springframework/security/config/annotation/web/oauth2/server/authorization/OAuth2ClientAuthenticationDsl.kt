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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2ClientAuthenticationConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure OAuth 2.0 Client Authentication using idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property authenticationSuccessHandler the [AuthenticationSuccessHandler] used for
 * handling a successful client authentication
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * failed client authentication
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2ClientAuthenticationDsl {
    var authenticationSuccessHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var authenticationConverter: AuthenticationConverter? = null
    private var authenticationConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract client
     * credentials from the request.
     *
     * @param authenticationConverter the [AuthenticationConverter] to add
     */
    fun authenticationConverter(authenticationConverter: AuthenticationConverter) {
        this.authenticationConverter = authenticationConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param authenticationConvertersConsumer the [Consumer] for the list of converters
     */
    fun authenticationConverters(authenticationConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.authenticationConvertersConsumer = authenticationConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating client credentials.
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

    internal fun get(): (OAuth2ClientAuthenticationConfigurer) -> Unit {
        return { clientAuthentication ->
            authenticationConverter?.also { clientAuthentication.authenticationConverter(authenticationConverter) }
            authenticationConvertersConsumer?.also { clientAuthentication.authenticationConverters(authenticationConvertersConsumer) }
            authenticationProvider?.also { clientAuthentication.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { clientAuthentication.authenticationProviders(authenticationProvidersConsumer) }
            authenticationSuccessHandler?.also { clientAuthentication.authenticationSuccessHandler(authenticationSuccessHandler) }
            errorResponseHandler?.also { clientAuthentication.errorResponseHandler(errorResponseHandler) }
        }
    }
}
