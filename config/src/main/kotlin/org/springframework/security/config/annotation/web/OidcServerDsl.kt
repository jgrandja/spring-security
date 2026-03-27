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

import org.springframework.security.config.annotation.web.SecurityMarker
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcLogoutEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcProviderConfigurationEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcUserInfoEndpointConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OidcClientRegistrationEndpointConfigurer
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OidcClientRegistrationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OidcLogoutEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OidcProviderConfigurationEndpointDsl
import org.springframework.security.config.annotation.web.oauth2.server.authorization.OidcUserInfoEndpointDsl

/**
 * A Kotlin DSL to configure OpenID Connect 1.0 support using idiomatic Kotlin code.
 *
 * @author Joe Grandja
 * @since 7.0
 */
@SecurityMarker
class OidcDsl {
    private var providerConfigurationEndpoint: ((OidcProviderConfigurationEndpointConfigurer) -> Unit)? = null
    private var logoutEndpoint: ((OidcLogoutEndpointConfigurer) -> Unit)? = null
    private var userInfoEndpoint: ((OidcUserInfoEndpointConfigurer) -> Unit)? = null
    private var clientRegistrationEndpoint: ((OidcClientRegistrationEndpointConfigurer) -> Unit)? = null

    /**
     * Configures the OpenID Connect 1.0 Provider Configuration Endpoint.
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
     *                     providerConfigurationEndpoint {
     *                         providerConfigurationCustomizer = { builder -> builder.claim("custom", "value") }
     *                     }
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param providerConfigurationEndpointConfig custom configurations to configure the
     * provider configuration endpoint
     * @see [OidcProviderConfigurationEndpointDsl]
     */
    fun providerConfigurationEndpoint(providerConfigurationEndpointConfig: OidcProviderConfigurationEndpointDsl.() -> Unit) {
        this.providerConfigurationEndpoint = OidcProviderConfigurationEndpointDsl().apply(providerConfigurationEndpointConfig).get()
    }

    /**
     * Configures the OpenID Connect 1.0 RP-Initiated Logout Endpoint.
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
     *                     logoutEndpoint { }
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param logoutEndpointConfig custom configurations to configure the logout endpoint
     * @see [OidcLogoutEndpointDsl]
     */
    fun logoutEndpoint(logoutEndpointConfig: OidcLogoutEndpointDsl.() -> Unit) {
        this.logoutEndpoint = OidcLogoutEndpointDsl().apply(logoutEndpointConfig).get()
    }

    /**
     * Configures the OpenID Connect 1.0 UserInfo Endpoint.
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
     * @param userInfoEndpointConfig custom configurations to configure the UserInfo endpoint
     * @see [OidcUserInfoEndpointDsl]
     */
    fun userInfoEndpoint(userInfoEndpointConfig: OidcUserInfoEndpointDsl.() -> Unit) {
        this.userInfoEndpoint = OidcUserInfoEndpointDsl().apply(userInfoEndpointConfig).get()
    }

    /**
     * Configures the OpenID Connect Dynamic Client Registration 1.0 Endpoint.
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
     *                     clientRegistrationEndpoint { }
     *                 }
     *             }
     *         }
     *         return http.build()
     *     }
     * }
     * ```
     *
     * @param clientRegistrationEndpointConfig custom configurations to configure the
     * client registration endpoint
     * @see [OidcClientRegistrationEndpointDsl]
     */
    fun clientRegistrationEndpoint(clientRegistrationEndpointConfig: OidcClientRegistrationEndpointDsl.() -> Unit) {
        this.clientRegistrationEndpoint = OidcClientRegistrationEndpointDsl().apply(clientRegistrationEndpointConfig).get()
    }

    internal fun get(): (OidcConfigurer) -> Unit {
        return { oidc ->
            providerConfigurationEndpoint?.also { oidc.providerConfigurationEndpoint(providerConfigurationEndpoint) }
            logoutEndpoint?.also { oidc.logoutEndpoint(logoutEndpoint) }
            userInfoEndpoint?.also { oidc.userInfoEndpoint(userInfoEndpoint) }
            clientRegistrationEndpoint?.also { oidc.clientRegistrationEndpoint(clientRegistrationEndpoint) }
        }
    }
}
