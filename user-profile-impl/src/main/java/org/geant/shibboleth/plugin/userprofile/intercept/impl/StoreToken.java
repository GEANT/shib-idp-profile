/*
 * Copyright (c) 2022-2025, GÉANT
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

import java.text.ParseException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokenImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokenImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.shibboleth.idp.plugin.oidc.op.messaging.context.AccessTokenContext;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;
import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;
import net.shibboleth.idp.plugin.oidc.op.token.support.RefreshTokenClaimsSet;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;

/**
 * Updates access token information in user profile cache.
 */
public class StoreToken extends AbstractUserProfileInterceptorAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(StoreToken.class);

    /** Data sealer for unwrapping authorization code. */
    @NonnullAfterInit
    private DataSealer dataSealer;

    /** Strategy used to locate the subcontext with the token. */
    @Nonnull
    private Function<ProfileRequestContext, AccessTokenContext> accessTokenContextLookupStrategy;

    /** oidc response context. */
    @Nullable
    private OIDCAuthenticationResponseContext oidcResponseContext;

    /** Token context. */
    @Nullable
    private AccessTokenContext tokenCtx;

    /** Constructor. */
    public StoreToken() {
        super();
        accessTokenContextLookupStrategy = new ChildContextLookup<>(AccessTokenContext.class)
                .compose(new ChildContextLookup<>(OIDCAuthenticationResponseContext.class)
                        .compose(new OutboundMessageContextLookup()));
    }

    /**
     * Set the strategy used to lookup the {@link AccessTokenContext} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setAccessTokenContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AccessTokenContext> strategy) {
        checkSetterPreconditions();
        accessTokenContextLookupStrategy = Constraint.isNotNull(strategy,
                "AccessTokenContext lookup strategy cannot be null");
    }

    /**
     * Set the data sealer instance to use.
     * 
     * @param sealer sealer to use
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        checkSetterPreconditions();
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (dataSealer == null) {
            throw new ComponentInitializationException("DataSealer cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        tokenCtx = accessTokenContextLookupStrategy.apply(profileRequestContext);
        if (tokenCtx == null) {
            log.debug("{} AccessTokenContext is missing", getLogPrefix());
            return false;
        }

        if (tokenCtx.getJWT() == null && tokenCtx.getOpaque() == null) {
            log.debug("{} Access token is missing", getLogPrefix());
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
        String user = usernameLookupStrategy.apply(profileRequestContext);
        try {
            Event event = userProfileCache.getSingleEvent(user, AccessTokens.ENTRY_NAME, userProfileCacheContext);
            AccessTokens accessTokens = event != null ? AccessTokens.parse(event.getValue()) : new AccessTokens();
            // TODO: take clockSkew into consideration
            accessTokens.getAccessTokens()
                    .removeIf(accessToken -> accessToken.getExp() < System.currentTimeMillis() / 1000);
            AccessTokenClaimsSet accessToken = tokenCtx.getJWT() != null
                    ? AccessTokenClaimsSet.parse(tokenCtx.getJWT(), dataSealer)
                    : AccessTokenClaimsSet.parse(tokenCtx.getOpaque(), dataSealer);
            accessTokens.getAccessTokens().add(new AccessTokenImpl(accessToken));
            userProfileCache.setSingleEvent(AccessTokens.ENTRY_NAME, accessTokens.serialize(), userProfileCacheContext);
            if (log.isDebugEnabled()) {
                log.debug("{} Updated access tokens {} ", getLogPrefix(), accessTokens.serialize());
            }
            String refreshToken = oidcResponseContext.getRefreshToken() != null
                    ? oidcResponseContext.getRefreshToken().getValue()
                    : null;
            if (refreshToken != null) {
                event = userProfileCache.getSingleEvent(user, RefreshTokens.ENTRY_NAME, userProfileCacheContext);
                RefreshTokens refreshTokens = event != null ? RefreshTokens.parse(event.getValue())
                        : new RefreshTokens();
                // TODO: take clockSkew into consideration
                refreshTokens.getRefreshTokens()
                        .removeIf(refToken -> refToken.getExp() < System.currentTimeMillis() / 1000);
                refreshTokens.getRefreshTokens()
                        .add(new RefreshTokenImpl(RefreshTokenClaimsSet.parse(refreshToken, dataSealer)));
                userProfileCache.setSingleEvent(RefreshTokens.ENTRY_NAME, refreshTokens.serialize(),
                        userProfileCacheContext);
                if (log.isDebugEnabled()) {
                    log.debug("{} Updated refresh tokens {} ", getLogPrefix(), refreshTokens.serialize());
                }
            }

        } catch (JsonProcessingException | ParseException | DataSealerException e) {
            log.error("{} Failed parsing token", getLogPrefix(), e);
            // We are intentionally not returning error.
        }
    }
}
