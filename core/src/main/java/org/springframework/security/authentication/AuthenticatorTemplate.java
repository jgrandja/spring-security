/*
 * Copyright 2002-2024 the original author or authors.
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
package org.springframework.security.authentication;

/**
 * A template for defining a custom authentication mechanism via an {@link Authenticator}.
 *
 * @author Joe Grandja
 */
public interface AuthenticatorTemplate<REQUEST_DATA, AUTHN_REQUEST extends AuthenticationRequest, AUTHN_RESULT extends AuthenticationResult, RESPONSE_DATA> {

	AUTHN_REQUEST requestConverter(REQUEST_DATA requestData);

	AUTHN_RESULT authenticator(AUTHN_REQUEST authenticationRequest);

	// FIXME This might need to change to support AuthenticationSuccessHandler and AuthenticationFailureHandler
	RESPONSE_DATA resultConverter(AUTHN_RESULT authenticationResult);

}
