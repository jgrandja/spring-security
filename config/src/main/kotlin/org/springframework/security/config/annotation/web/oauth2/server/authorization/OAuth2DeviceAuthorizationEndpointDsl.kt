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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2DeviceAuthorizationEndpointConfigurer
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * A Kotlin DSL to configure the OAuth 2.0 Device Authorization Endpoint using idiomatic
 * Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property deviceAuthorizationResponseHandler the [AuthenticationSuccessHandler] used
 * for handling a device authorization request and returning the Device Authorization
 * Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * device authorization request error and returning the Error Response
 * @property verificationUri the end-user verification URI on the authorization server
 */
@OAuth2AuthorizationServerSecurityMarker
class OAuth2DeviceAuthorizationEndpointDsl {
    var deviceAuthorizationResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null
    var verificationUri: String? = null

    private var deviceAuthorizationRequestConverter: AuthenticationConverter? = null
    private var deviceAuthorizationRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a Device
     * Authorization Request from the request.
     *
     * @param deviceAuthorizationRequestConverter the [AuthenticationConverter] to add
     */
    fun deviceAuthorizationRequestConverter(deviceAuthorizationRequestConverter: AuthenticationConverter) {
        this.deviceAuthorizationRequestConverter = deviceAuthorizationRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param deviceAuthorizationRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun deviceAuthorizationRequestConverters(deviceAuthorizationRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.deviceAuthorizationRequestConvertersConsumer = deviceAuthorizationRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a device authorization
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

    internal fun get(): (OAuth2DeviceAuthorizationEndpointConfigurer) -> Unit {
        return { deviceAuthorizationEndpoint ->
            deviceAuthorizationRequestConverter?.also { deviceAuthorizationEndpoint.deviceAuthorizationRequestConverter(deviceAuthorizationRequestConverter) }
            deviceAuthorizationRequestConvertersConsumer?.also { deviceAuthorizationEndpoint.deviceAuthorizationRequestConverters(deviceAuthorizationRequestConvertersConsumer) }
            authenticationProvider?.also { deviceAuthorizationEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { deviceAuthorizationEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            deviceAuthorizationResponseHandler?.also { deviceAuthorizationEndpoint.deviceAuthorizationResponseHandler(deviceAuthorizationResponseHandler) }
            errorResponseHandler?.also { deviceAuthorizationEndpoint.errorResponseHandler(errorResponseHandler) }
            verificationUri?.also { deviceAuthorizationEndpoint.verificationUri(verificationUri) }
        }
    }
}
