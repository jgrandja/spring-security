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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2DeviceVerificationEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Device Verification Endpoint using idiomatic
 * Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property deviceVerificationResponseHandler the [AuthenticationSuccessHandler] used
 * for handling a device verification request and returning the response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * device verification request error and returning the Error Response
 * @property consentPage the URI of the custom consent page to redirect to if consent is
 * required (e.g. "/oauth2/consent")
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2DeviceVerificationEndpointDsl {
    var deviceVerificationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null
    var consentPage: String? = null

    private var deviceVerificationRequestConverter: AuthenticationConverter? = null
    private var deviceVerificationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Device
     * Verification Request from the request.
     *
     * @param deviceVerificationRequestConverter the [AuthenticationConverter] to add
     */
    fun deviceVerificationRequestConverter(deviceVerificationRequestConverter: AuthenticationConverter) {
        this.deviceVerificationRequestConverter = deviceVerificationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param deviceVerificationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun deviceVerificationRequestConverters(deviceVerificationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.deviceVerificationRequestConvertersConsumer = deviceVerificationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a device verification
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

    internal fun get(): (OAuth2DeviceVerificationEndpointConfigurer) -> Unit {
        return { deviceVerificationEndpoint ->
            deviceVerificationRequestConverter?.also { deviceVerificationEndpoint.deviceVerificationRequestConverter(deviceVerificationRequestConverter) }
            deviceVerificationRequestConvertersConsumer?.also { deviceVerificationEndpoint.deviceVerificationRequestConverters(deviceVerificationRequestConvertersConsumer) }
            authenticationProvider?.also { deviceVerificationEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { deviceVerificationEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            deviceVerificationResponseHandler?.also { deviceVerificationEndpoint.deviceVerificationResponseHandler(deviceVerificationResponseHandler) }
            errorResponseHandler?.also { deviceVerificationEndpoint.errorResponseHandler(errorResponseHandler) }
            consentPage?.also { deviceVerificationEndpoint.consentPage(consentPage) }
        }
    }
}
