/*
 * Copyright (c) 2022-2023, GÉANT
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

package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServices;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.shibboleth.idp.plugin.oidc.op.storage.RevocationCacheContexts;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * Actions reads token, connected organization and login events from user
 * profile cache. The data is stored to {@link UserProfileContext}
 */
public class RenderUserProfileCacheItems extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderUserProfileCacheItems.class);

    /** Token revocation cache instance to use. */
    @Nullable
    private RevocationCache revocationCache;

    /** Context for user profile . */
    private UserProfileContext userProfileContext;

    /**
     * Lookup strategy for user name principal.
     */
    @NonnullAfterInit
    protected Function<ProfileRequestContext, String> usernameLookupStrategy;

    /**
     * User profile cache.
     */
    @NonnullAfterInit
    protected UserProfileCache userProfileCache;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Constructor. */
    public RenderUserProfileCacheItems() {
        super();
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
    }

    /**
     * Set the revocation cache instance to use.
     * 
     * @param cache The revocationCache to set.
     */
    public void setRevocationCache(@Nonnull final RevocationCache cache) {
        checkSetterPreconditions();
        revocationCache = cache;
    }

    /**
     * Set user profile cache.
     * 
     * @param cache user profile cache
     */
    public void setUserProfileCache(@Nonnull final UserProfileCache cache) {
        checkSetterPreconditions();
        userProfileCache = Constraint.isNotNull(cache, "UserProfileCache cannot be null");
    }

    /**
     * Set Lookup strategy for user name.
     * 
     * @param strategy lookup strategy for user name
     */
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }

    /**
     * Set strategy used to locate or create the {@link UserProfileContext} to
     * populate.
     * 
     * @param strategy Strategy used to locate or create the
     *                 {@link UserProfileContext} to populate.
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        checkSetterPreconditions();
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (userProfileCache == null) {
            throw new ComponentInitializationException("UserProfileCache cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        if (usernameLookupStrategy.apply(profileRequestContext) == null || usernameLookupStrategy.apply(profileRequestContext).isEmpty()) {
            log.warn("{} No username", getLogPrefix());
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        String user = usernameLookupStrategy.apply(profileRequestContext);
        Event event = null;
        // If there is no token revocation cache we ignore tokens.
        if (revocationCache != null) {
            event = userProfileCache.getSingleEvent(user, AccessTokens.ENTRY_NAME);
            try {
                userProfileContext.getAccessTokens().clear();
                AccessTokens tokens = event != null ? AccessTokens.parse(event.getValue()) : new AccessTokens();
                // TODO: take clockSkew into consideration
                tokens.getAccessTokens()
                        .removeIf(accessToken -> accessToken.getExp() < System.currentTimeMillis() / 1000);
                userProfileCache.setSingleEvent(user, AccessTokens.ENTRY_NAME, tokens.serialize());
                log.debug("{} Updated access tokens {} ", getLogPrefix(), tokens.serialize());
                // Remove all revoked tokens from tokens displayed.
                tokens.getAccessTokens().removeIf(accessToken -> revocationCache
                        .isRevoked(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, accessToken.getTokenId()));
                tokens.getAccessTokens().removeIf(accessToken -> revocationCache
                        .isRevoked(RevocationCacheContexts.AUTHORIZATION_CODE, accessToken.getTokenRootId()));
                tokens.getAccessTokens().forEach(
                        (accessToken -> userProfileContext.addAccessToken(accessToken.getClientId(), accessToken)));
            } catch (JsonProcessingException e) {
                log.error("{} Failed processing access tokens.", getLogPrefix(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                return;
            }
            event = userProfileCache.getSingleEvent(user, RefreshTokens.ENTRY_NAME);
            try {
                userProfileContext.getRefreshTokens().clear();
                RefreshTokens tokens = event != null ? RefreshTokens.parse(event.getValue()) : new RefreshTokens();
                // TODO: take clockSkew into consideration
                tokens.getRefreshTokens()
                        .removeIf(refreshToken -> refreshToken.getExp() < System.currentTimeMillis() / 1000);
                userProfileCache.setSingleEvent(user, RefreshTokens.ENTRY_NAME, tokens.serialize());
                log.debug("{} Updated refresh tokens {} ", getLogPrefix(), tokens.serialize());
                // Remove all revoked tokens from tokens displayed.
                tokens.getRefreshTokens().removeIf(refreshToken -> revocationCache
                        .isRevoked(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, refreshToken.getTokenId()));
                tokens.getRefreshTokens().removeIf(refreshToken -> revocationCache
                        .isRevoked(RevocationCacheContexts.AUTHORIZATION_CODE, refreshToken.getTokenRootId()));
                tokens.getRefreshTokens().forEach(
                        (refreshToken -> userProfileContext.addRefreshToken(refreshToken.getClientId(), refreshToken)));
            } catch (JsonProcessingException e) {
                log.error("{} Failed processing refresh tokens.", getLogPrefix(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
                return;
            }
        }
        event = userProfileCache.getSingleEvent(user, ConnectedServices.ENTRY_NAME);
        try {
            userProfileContext.getConnectedOrganizations().clear();
            ConnectedServices organizations = event != null ? ConnectedServices.parse(event.getValue())
                    : new ConnectedServices();
            organizations.getConnectedServices().forEach((rpId, connectedOrganization) -> userProfileContext
                    .getConnectedOrganizations().put(rpId, connectedOrganization));
        } catch (JsonProcessingException e) {
            log.error("{} Failed processing connected organizations.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
        }
        event = userProfileCache.getSingleEvent(user, LoginEvents.ENTRY_NAME);
        try {
            LoginEvents organizations = event != null ? LoginEvents.parse(event.getValue()) : new LoginEvents();
            userProfileContext.getLoginEvents().clear();
            userProfileContext.getLoginEvents().addAll(organizations.getLoginEvents());
        } catch (JsonProcessingException e) {
            log.error("{} Failed processing connected organizations.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
        }
    }
}
