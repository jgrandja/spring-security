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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcUserInfoEndpointConfigurer
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import java.util.function.Function

/**
 * A Kotlin DSL to configure the OpenID Connect 1.0 UserInfo Endpoint using idiomatic
 * Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property userInfoResponseHandler the [AuthenticationSuccessHandler] used for handling
 * a UserInfo request and returning the UserInfo Response
 * @property errorResponseHandler the [AuthenticationFailureHandler] used for handling a
 * UserInfo request error and returning the Error Response
 */
@OAuth2AuthorizationServerSecurityMarker
class OidcUserInfoEndpointDsl {
    var userInfoResponseHandler: AuthenticationSuccessHandler? = null
    var errorResponseHandler: AuthenticationFailureHandler? = null

    private var userInfoRequestConverter: AuthenticationConverter? = null
    private var userInfoRequestConvertersConsumer: ((MutableList<AuthenticationConverter>) -> Unit)? = null
    private var authenticationProvider: AuthenticationProvider? = null
    private var authenticationProvidersConsumer: ((MutableList<AuthenticationProvider>) -> Unit)? = null
    private var userInfoMapper: Function<OidcUserInfoAuthenticationContext, OidcUserInfo>? = null

    /**
     * Adds an [AuthenticationConverter] used when attempting to extract a UserInfo
     * Request from the request.
     *
     * @param userInfoRequestConverter the [AuthenticationConverter] to add
     */
    fun userInfoRequestConverter(userInfoRequestConverter: AuthenticationConverter) {
        this.userInfoRequestConverter = userInfoRequestConverter
    }

    /**
     * Sets the [Consumer] providing access to the [List] of default and added
     * [AuthenticationConverter]'s.
     *
     * @param userInfoRequestConvertersConsumer the [Consumer] for the list of converters
     */
    fun userInfoRequestConverters(userInfoRequestConvertersConsumer: (MutableList<AuthenticationConverter>) -> Unit) {
        this.userInfoRequestConvertersConsumer = userInfoRequestConvertersConsumer
    }

    /**
     * Adds an [AuthenticationProvider] used for authenticating a UserInfo request.
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

    /**
     * Sets the [Function] used to extract claims from [OidcUserInfoAuthenticationContext]
     * to an instance of [OidcUserInfo] for the UserInfo response.
     *
     * @param userInfoMapper the [Function] used to extract claims
     */
    fun userInfoMapper(userInfoMapper: Function<OidcUserInfoAuthenticationContext, OidcUserInfo>) {
        this.userInfoMapper = userInfoMapper
    }

    internal fun get(): (OidcUserInfoEndpointConfigurer) -> Unit {
        return { userInfoEndpoint ->
            userInfoRequestConverter?.also { userInfoEndpoint.userInfoRequestConverter(userInfoRequestConverter) }
            userInfoRequestConvertersConsumer?.also { userInfoEndpoint.userInfoRequestConverters(userInfoRequestConvertersConsumer) }
            authenticationProvider?.also { userInfoEndpoint.authenticationProvider(authenticationProvider) }
            authenticationProvidersConsumer?.also { userInfoEndpoint.authenticationProviders(authenticationProvidersConsumer) }
            userInfoResponseHandler?.also { userInfoEndpoint.userInfoResponseHandler(userInfoResponseHandler) }
            errorResponseHandler?.also { userInfoEndpoint.errorResponseHandler(errorResponseHandler) }
            userInfoMapper?.also { userInfoEndpoint.userInfoMapper(userInfoMapper) }
        }
    }
}
