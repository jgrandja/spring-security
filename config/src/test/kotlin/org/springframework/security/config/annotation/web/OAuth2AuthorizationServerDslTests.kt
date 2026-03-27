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

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.test.SpringTestContext
import org.springframework.security.config.test.SpringTestContextExtension
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.jose.TestJwks
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.TestRegisteredClients
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Tests for [OAuth2AuthorizationServerDsl].
 *
 * @author Joe Grandja
 */
@ExtendWith(SpringTestContextExtension::class)
class OAuth2AuthorizationServerDslTests {

    @JvmField
    val spring = SpringTestContext(this)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var registeredClientRepository: RegisteredClientRepository

    companion object {
        private const val DEFAULT_TOKEN_ENDPOINT_URI = "/oauth2/token"

        lateinit var db: EmbeddedDatabase
        lateinit var jwkSource: JWKSource<SecurityContext>
        val authenticationConverter: AuthenticationConverter = mockk()
        val authenticationProvider: AuthenticationProvider = mockk()
        val authenticationSuccessHandler: AuthenticationSuccessHandler = mockk()
        val authenticationFailureHandler: AuthenticationFailureHandler = mockk()

        @JvmStatic
        @BeforeAll
        fun init() {
            val jwkSet = JWKSet(TestJwks.DEFAULT_RSA_JWK)
            jwkSource = JWKSource { jwkSelector, _ -> jwkSelector.select(jwkSet) }
            db = EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.HSQL)
                .setScriptEncoding("UTF-8")
                .addScript("org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql")
                .addScript("org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql")
                .build()
        }

        @JvmStatic
        @AfterAll
        fun destroy() {
            db.shutdown()
        }
    }

    @AfterEach
    fun tearDown() {
        if (this::registeredClientRepository.isInitialized) {
            val jdbcOperations = JdbcTemplate(db)
            jdbcOperations.update("truncate table oauth2_authorization")
            jdbcOperations.update("truncate table oauth2_registered_client")
        }
    }

    @Test
    fun `oauth2AuthorizationServer when token request not authenticated then unauthorized`() {
        spring.register(AuthorizationServerConfiguration::class.java).autowire()

        mockMvc.post(DEFAULT_TOKEN_ENDPOINT_URI) {
            param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.value)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `oauth2AuthorizationServer when token request valid then token response`() {
        spring.register(AuthorizationServerConfiguration::class.java).autowire()

        val registeredClient = TestRegisteredClients.registeredClient2().clientSecret("{noop}secret-2").build()
        registeredClientRepository.save(registeredClient)

        mockMvc.post(DEFAULT_TOKEN_ENDPOINT_URI) {
            param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.value)
            param(OAuth2ParameterNames.SCOPE, "scope1 scope2")
            header(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(registeredClient.clientId, "secret-2"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.access_token") { isNotEmpty() }
        }
    }

    @Test
    fun `oauth2AuthorizationServer when token endpoint customized then used`() {
        spring.register(AuthorizationServerConfigurationCustomTokenEndpoint::class.java).autowire()

        val registeredClient = TestRegisteredClients.registeredClient2().clientSecret("{noop}secret-2").build()
        registeredClientRepository.save(registeredClient)

        val clientPrincipal = OAuth2ClientAuthenticationToken(
            registeredClient,
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            registeredClient.clientSecret
        )
        val clientCredentialsAuthentication = OAuth2ClientCredentialsAuthenticationToken(clientPrincipal, null, null)
        every { authenticationConverter.convert(any()) } returns clientCredentialsAuthentication

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "token",
            Instant.now(), Instant.now().plus(Duration.ofHours(1))
        )
        val accessTokenAuthentication = OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken)
        every { authenticationProvider.supports(OAuth2ClientCredentialsAuthenticationToken::class.java) } returns true
        every { authenticationProvider.authenticate(any()) } returns accessTokenAuthentication
        justRun { authenticationSuccessHandler.onAuthenticationSuccess(any(), any(), any()) }

        mockMvc.post(DEFAULT_TOKEN_ENDPOINT_URI) {
            param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.value)
            header(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(registeredClient.clientId, "secret-2"))
        }.andExpect {
            status { isOk() }
        }

        verify(exactly = 1) { authenticationConverter.convert(any()) }
    }

    @Test
    fun `oauth2AuthorizationServer when custom authorization endpoint consent page then configured`() {
        spring.register(AuthorizationServerConfigurationCustomConsentPage::class.java).autowire()
        assertThat(spring.context).isNotNull()
    }

    @Test
    fun `oauth2AuthorizationServer when oidc enabled then oidc endpoints accessible`() {
        spring.register(AuthorizationServerConfigurationWithOidc::class.java).autowire()
        assertThat(spring.context).isNotNull()
    }

    private fun encodeBasicAuth(clientId: String, clientSecret: String?): String {
        val credentials = "$clientId:${clientSecret ?: ""}"
        return Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
    }

    @EnableWebSecurity
    @Configuration(proxyBeanMethods = false)
    open class AuthorizationServerConfiguration {

        @Bean
        open fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http {
                oauth2AuthorizationServer { }
                authorizeHttpRequests {
                    authorize(anyRequest, authenticated)
                }
            }
            return http.build()
        }

        @Bean
        open fun authorizationService(
            jdbcOperations: JdbcOperations,
            registeredClientRepository: RegisteredClientRepository
        ): OAuth2AuthorizationService {
            return JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository)
        }

        @Bean
        @Suppress("removal")
        open fun registeredClientRepository(jdbcOperations: JdbcOperations): RegisteredClientRepository {
            return JdbcRegisteredClientRepository(jdbcOperations)
        }

        @Bean
        open fun jdbcOperations(): JdbcOperations {
            return JdbcTemplate(db)
        }

        @Bean
        open fun jwkSource(): JWKSource<SecurityContext> {
            return jwkSource
        }

        @Bean
        open fun authorizationServerSettings(): AuthorizationServerSettings {
            return AuthorizationServerSettings.builder().build()
        }
    }

    @EnableWebSecurity
    @Configuration(proxyBeanMethods = false)
    open class AuthorizationServerConfigurationCustomTokenEndpoint : AuthorizationServerConfiguration() {

        @Bean
        override fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http {
                oauth2AuthorizationServer {
                    tokenEndpoint {
                        accessTokenRequestConverter(authenticationConverter)
                        authenticationProvider(authenticationProvider)
                        accessTokenResponseHandler = authenticationSuccessHandler
                        errorResponseHandler = authenticationFailureHandler
                    }
                }
                authorizeHttpRequests {
                    authorize(anyRequest, authenticated)
                }
            }
            return http.build()
        }
    }

    @EnableWebSecurity
    @Configuration(proxyBeanMethods = false)
    open class AuthorizationServerConfigurationCustomConsentPage : AuthorizationServerConfiguration() {

        @Bean
        override fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http {
                oauth2AuthorizationServer {
                    authorizationEndpoint {
                        consentPage = "/oauth2/consent"
                    }
                }
                authorizeHttpRequests {
                    authorize(anyRequest, authenticated)
                }
            }
            return http.build()
        }
    }

    @EnableWebSecurity
    @Configuration(proxyBeanMethods = false)
    open class AuthorizationServerConfigurationWithOidc : AuthorizationServerConfiguration() {

        @Bean
        override fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http {
                oauth2AuthorizationServer {
                    oidc {
                        providerConfigurationEndpoint { }
                        logoutEndpoint { }
                        userInfoEndpoint { }
                    }
                }
                authorizeHttpRequests {
                    authorize(anyRequest, authenticated)
                }
            }
            return http.build()
        }

        @Bean
        open fun jwtDecoder(): JwtDecoder = mockk()
    }
}
