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

package org.springframework.security.config.annotation.web

import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerMetadataEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2ClientAuthenticationConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2ClientRegistrationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2DeviceAuthorizationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2DeviceVerificationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2PushedAuthorizationRequestEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenIntrospectionEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenRevocationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcConfigurer
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2AuthorizationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2AuthorizationServerMetadataEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2ClientAuthenticationDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2ClientRegistrationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2DeviceAuthorizationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2DeviceVerificationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2PushedAuthorizationRequestEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2TokenEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2TokenIntrospectionEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OAuth2TokenRevocationEndpointDsl
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.core.OAuth2Token

/**
 * A Kotlin DSL to configure [HttpSecurity] OAuth 2.1 Authorization Server support using
 * idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 * @property registeredClientRepository the repository of registered clients
 * @property authorizationService the authorization service
 * @property authorizationConsentService the authorization consent service
 * @property authorizationServerSettings the authorization server settings
 * @property tokenGenerator the token generator
 */
@SecurityMarker
class OAuth2AuthorizationServerDsl {
    var registeredClientRepository: RegisteredClientRepository? = null
    var authorizationService: OAuth2AuthorizationService? = null
    var authorizationConsentService: OAuth2AuthorizationConsentService? = null
    var authorizationServerSettings: AuthorizationServerSettings? = null
    var tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>? = null

    private var clientAuthentication: ((OAuth2ClientAuthenticationConfigurer) -> Unit)? = null
    private var authorizationServerMetadataEndpoint: ((OAuth2AuthorizationServerMetadataEndpointConfigurer) -> Unit)? = null
    private var authorizationEndpoint: ((OAuth2AuthorizationEndpointConfigurer) -> Unit)? = null
    private var pushedAuthorizationRequestEndpoint: ((OAuth2PushedAuthorizationRequestEndpointConfigurer) -> Unit)? = null
    private var tokenEndpoint: ((OAuth2TokenEndpointConfigurer) -> Unit)? = null
    private var tokenIntrospectionEndpoint: ((OAuth2TokenIntrospectionEndpointConfigurer) -> Unit)? = null
    private var tokenRevocationEndpoint: ((OAuth2TokenRevocationEndpointConfigurer) -> Unit)? = null
    private var deviceAuthorizationEndpoint: ((OAuth2DeviceAuthorizationEndpointConfigurer) -> Unit)? = null
    private var deviceVerificationEndpoint: ((OAuth2DeviceVerificationEndpointConfigurer) -> Unit)? = null
    private var clientRegistrationEndpoint: ((OAuth2ClientRegistrationEndpointConfigurer) -> Unit)? = null
    private var oidc: ((OidcConfigurer) -> Unit)? = null

    /**
     * Configures OAuth 2.0 Client Authentication.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 clientAuthentication {
     *                     authenticationSuccessHandler = mySuccessHandler()
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param clientAuthenticationConfig custom configurations to configure client
     * authentication
     * @see [OAuth2ClientAuthenticationDsl]
     */
    fun clientAuthentication(clientAuthenticationConfig: OAuth2ClientAuthenticationDsl.() -> Unit) {
        this.clientAuthentication = OAuth2ClientAuthenticationDsl().apply(clientAuthenticationConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Authorization Server Metadata Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 authorizationServerMetadataEndpoint { }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param authorizationServerMetadataEndpointConfig custom configurations to configure
     * the authorization server metadata endpoint
     * @see [OAuth2AuthorizationServerMetadataEndpointDsl]
     */
    fun authorizationServerMetadataEndpoint(authorizationServerMetadataEndpointConfig: OAuth2AuthorizationServerMetadataEndpointDsl.() -> Unit) {
        this.authorizationServerMetadataEndpoint = OAuth2AuthorizationServerMetadataEndpointDsl().apply(authorizationServerMetadataEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Authorization Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 authorizationEndpoint {
     *                     consentPage = "/oauth2/consent"
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param authorizationEndpointConfig custom configurations to configure the
     * authorization endpoint
     * @see [OAuth2AuthorizationEndpointDsl]
     */
    fun authorizationEndpoint(authorizationEndpointConfig: OAuth2AuthorizationEndpointDsl.() -> Unit) {
        this.authorizationEndpoint = OAuth2AuthorizationEndpointDsl().apply(authorizationEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Pushed Authorization Request Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 pushedAuthorizationRequestEndpoint { }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param pushedAuthorizationRequestEndpointConfig custom configurations to configure
     * the pushed authorization request endpoint
     * @see [OAuth2PushedAuthorizationRequestEndpointDsl]
     */
    fun pushedAuthorizationRequestEndpoint(pushedAuthorizationRequestEndpointConfig: OAuth2PushedAuthorizationRequestEndpointDsl.() -> Unit) {
        this.pushedAuthorizationRequestEndpoint = OAuth2PushedAuthorizationRequestEndpointDsl().apply(pushedAuthorizationRequestEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Token Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 tokenEndpoint {
     *                     accessTokenResponseHandler = myAccessTokenResponseHandler()
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param tokenEndpointConfig custom configurations to configure the token endpoint
     * @see [OAuth2TokenEndpointDsl]
     */
    fun tokenEndpoint(tokenEndpointConfig: OAuth2TokenEndpointDsl.() -> Unit) {
        this.tokenEndpoint = OAuth2TokenEndpointDsl().apply(tokenEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Token Introspection Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 tokenIntrospectionEndpoint { }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param tokenIntrospectionEndpointConfig custom configurations to configure the
     * token introspection endpoint
     * @see [OAuth2TokenIntrospectionEndpointDsl]
     */
    fun tokenIntrospectionEndpoint(tokenIntrospectionEndpointConfig: OAuth2TokenIntrospectionEndpointDsl.() -> Unit) {
        this.tokenIntrospectionEndpoint = OAuth2TokenIntrospectionEndpointDsl().apply(tokenIntrospectionEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Token Revocation Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 tokenRevocationEndpoint { }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param tokenRevocationEndpointConfig custom configurations to configure the token
     * revocation endpoint
     * @see [OAuth2TokenRevocationEndpointDsl]
     */
    fun tokenRevocationEndpoint(tokenRevocationEndpointConfig: OAuth2TokenRevocationEndpointDsl.() -> Unit) {
        this.tokenRevocationEndpoint = OAuth2TokenRevocationEndpointDsl().apply(tokenRevocationEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Device Authorization Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 deviceAuthorizationEndpoint {
     *                     verificationUri = "/activate"
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param deviceAuthorizationEndpointConfig custom configurations to configure the
     * device authorization endpoint
     * @see [OAuth2DeviceAuthorizationEndpointDsl]
     */
    fun deviceAuthorizationEndpoint(deviceAuthorizationEndpointConfig: OAuth2DeviceAuthorizationEndpointDsl.() -> Unit) {
        this.deviceAuthorizationEndpoint = OAuth2DeviceAuthorizationEndpointDsl().apply(deviceAuthorizationEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Device Verification Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 deviceVerificationEndpoint {
     *                     consentPage = "/oauth2/consent"
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param deviceVerificationEndpointConfig custom configurations to configure the
     * device verification endpoint
     * @see [OAuth2DeviceVerificationEndpointDsl]
     */
    fun deviceVerificationEndpoint(deviceVerificationEndpointConfig: OAuth2DeviceVerificationEndpointDsl.() -> Unit) {
        this.deviceVerificationEndpoint = OAuth2DeviceVerificationEndpointDsl().apply(deviceVerificationEndpointConfig).get()
    }

    /**
     * Configures the OAuth 2.0 Dynamic Client Registration Endpoint.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 clientRegistrationEndpoint { }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param clientRegistrationEndpointConfig custom configurations to configure the
     * dynamic client registration endpoint
     * @see [OAuth2ClientRegistrationEndpointDsl]
     */
    fun clientRegistrationEndpoint(clientRegistrationEndpointConfig: OAuth2ClientRegistrationEndpointDsl.() -> Unit) {
        this.clientRegistrationEndpoint = OAuth2ClientRegistrationEndpointDsl().apply(clientRegistrationEndpointConfig).get()
    }

    /**
     * Configures OpenID Connect 1.0 support.
     *
     * Example:
     *
     * ```
     * @Configuration
     * @EnableWebSecurity
     * class SecurityConfig {
     *
     *     @Bean
     *     fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
     *         http {
     *             oauth2AuthorizationServer {
     *                 oidc {
     *                     userInfoEndpoint { }
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param oidcConfig custom configurations to configure OpenID Connect 1.0 support
     * @see [OidcDsl]
     */
    fun oidc(oidcConfig: OidcDsl.() -> Unit) {
        this.oidc = OidcDsl().apply(oidcConfig).get()
    }

    internal fun get(): (OAuth2AuthorizationServerConfigurer) -> Unit {
        return { authorizationServer ->
            registeredClientRepository?.also { authorizationServer.registeredClientRepository(registeredClientRepository) }
            authorizationService?.also { authorizationServer.authorizationService(authorizationService) }
            authorizationConsentService?.also { authorizationServer.authorizationConsentService(authorizationConsentService) }
            authorizationServerSettings?.also { authorizationServer.authorizationServerSettings(authorizationServerSettings) }
            tokenGenerator?.also { authorizationServer.tokenGenerator(tokenGenerator) }
            clientAuthentication?.also { authorizationServer.clientAuthentication(clientAuthentication) }
            authorizationServerMetadataEndpoint?.also { authorizationServer.authorizationServerMetadataEndpoint(authorizationServerMetadataEndpoint) }
            authorizationEndpoint?.also { authorizationServer.authorizationEndpoint(authorizationEndpoint) }
            pushedAuthorizationRequestEndpoint?.also { authorizationServer.pushedAuthorizationRequestEndpoint(pushedAuthorizationRequestEndpoint) }
            tokenEndpoint?.also { authorizationServer.tokenEndpoint(tokenEndpoint) }
            tokenIntrospectionEndpoint?.also { authorizationServer.tokenIntrospectionEndpoint(tokenIntrospectionEndpoint) }
            tokenRevocationEndpoint?.also { authorizationServer.tokenRevocationEndpoint(tokenRevocationEndpoint) }
            deviceAuthorizationEndpoint?.also { authorizationServer.deviceAuthorizationEndpoint(deviceAuthorizationEndpoint) }
            deviceVerificationEndpoint?.also { authorizationServer.deviceVerificationEndpoint(deviceVerificationEndpoint) }
            clientRegistrationEndpoint?.also { authorizationServer.clientRegistrationEndpoint(clientRegistrationEndpoint) }
            oidc?.also { authorizationServer.oidc(oidc) }
        }
    }
}
