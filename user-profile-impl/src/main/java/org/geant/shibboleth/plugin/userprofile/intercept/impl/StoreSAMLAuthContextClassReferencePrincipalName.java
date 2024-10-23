/*
 * Copyright (c) 2024, GÉANT
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores authentication class reference name to
 * {@link UserProfileCacheContext}.
 */
public class StoreSAMLAuthContextClassReferencePrincipalName extends AbstractUserProfileInterceptorAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(StoreSAMLAuthContextClassReferencePrincipalName.class);

    /** Assertion to look ACR for. */
    private Assertion assertion;

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        assertion = new AssertionStrategy().apply(profileRequestContext);
        if (assertion == null) {
            log.warn("{} No assertion to look ACR for", getLogPrefix());
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        for (AuthnStatement statement : assertion.getAuthnStatements()) {
            if (statement.getAuthnContext() != null && statement.getAuthnContext().getAuthnContextClassRef() != null) {
                userProfileCacheContext.setAuthnContextClassReferencePrincipalName(
                        statement.getAuthnContext().getAuthnContextClassRef().getURI());
                break;
            }
            if (statement.getAuthnContext() != null && statement.getAuthnContext().getAuthnContextDeclRef() != null) {
                userProfileCacheContext.setAuthnContextClassReferencePrincipalName(
                        statement.getAuthnContext().getAuthnContextDeclRef().getURI());
                break;
            }

        }
        log.debug("{} ACR stored to context as {}", getLogPrefix(),
                userProfileCacheContext.getAuthnContextClassReferencePrincipalName());
    }

    private class AssertionStrategy implements Function<ProfileRequestContext, Assertion> {

        /** {@inheritDoc} */
        @Nullable
        public Assertion apply(@Nullable final ProfileRequestContext input) {
            final MessageContext omc = input != null ? input.getOutboundMessageContext() : null;

            if (omc != null) {
                final Object outboundMessage = omc.getMessage();
                if (outboundMessage == null) {
                    return null;
                } else if (outboundMessage instanceof Assertion) {
                    return (Assertion) outboundMessage;
                } else if (outboundMessage instanceof Response
                        && ((Response) outboundMessage).getAssertions().size() > 0) {
                    return ((Response) outboundMessage).getAssertions().get(0);
                }
            }

            return null;
        }
    }
}