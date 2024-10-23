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

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;

/**
 * Stores authentication class reference name to
 * {@link UserProfileCacheContext}.
 */
public class StoreOIDCAuthContextClassReferencePrincipalName extends AbstractUserProfileInterceptorAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(StoreOIDCAuthContextClassReferencePrincipalName.class);

    /** oidc response context. */
    @Nonnull
    private OIDCAuthenticationResponseContext oidcResponseContext;

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        final MessageContext outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        if (outboundMessageCtx == null) {
            log.warn("{} No outbound message context", getLogPrefix());
            return false;
        }
        oidcResponseContext = outboundMessageCtx.getSubcontext(OIDCAuthenticationResponseContext.class);
        if (oidcResponseContext == null) {
            log.warn("{} No OIDC response context", getLogPrefix());
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        userProfileCacheContext.setAuthnContextClassReferencePrincipalName(
                oidcResponseContext.getAcr() != null ? oidcResponseContext.getAcr().getValue() : null);
        log.debug("{} ACR stored to context as {}", getLogPrefix(),
                userProfileCacheContext.getAuthnContextClassReferencePrincipalName());
    }
}