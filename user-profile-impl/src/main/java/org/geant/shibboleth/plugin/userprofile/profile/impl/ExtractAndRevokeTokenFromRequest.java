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

package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.api.Token;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.plugin.oidc.op.storage.RevocationCacheContexts;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.oidc.profile.core.OidcEventIds;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.profile.action.ActionSupport;

import net.shibboleth.shared.primitive.StringSupport;

/**
 * An action that extracts a token id from HTTP form. The extracted token id is
 * first verified to be user's and then revoked.
 */

public class ExtractAndRevokeTokenFromRequest extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(ExtractAndRevokeTokenFromRequest.class);

    /**
     * Positive clock skew adjustment to consider when calculating revocation
     * lifetime.
     */
    @Nonnull
    private Duration clockSkew;

    /** Token revocation cache instance to use. */
    @Nullable
    private RevocationCache revocationCache;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Parameter name for token id. */
    @Nonnull
    @NotEmpty
    private String tokenIdFieldName;

    /** Extracted token. */
    private Token token;

    /** Constructor. */
    ExtractAndRevokeTokenFromRequest() {
        tokenIdFieldName = "_eventId_revokeToken";
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
        clockSkew = Duration.ofMinutes(5);
    }

    /**
     * Set the clock skew.
     * 
     * @param skew clock skew to set
     */
    public void setClockSkew(@Nonnull final Duration skew) {
        clockSkew = Constraint.isNotNull(skew, "Clock skew cannot be null").abs();
    }

    /**
     * Set the revocation cache instance to use.
     * 
     * @param cache revocation cache
     */
    public void setRevocationCache(@Nonnull final RevocationCache cache) {
        checkSetterPreconditions();
        revocationCache = cache;
    }

    /**
     * Set parameter name for token id.
     * 
     * @param fieldName parameter name for token id
     */
    public void setTokenIdFieldName(@Nonnull @NotEmpty final String fieldName) {
        checkSetterPreconditions();
        tokenIdFieldName = Constraint.isNotNull(StringSupport.trimOrNull(fieldName),
                "Access token id field name cannot be null or empty.");
    }

    /**
     * Set strategy used to locate or create the {@link UserProfileContext} to
     * populate.
     * 
     * @param strategy strategy used to locate the {@link UserProfileContext} to
     *                 populate
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        checkSetterPreconditions();
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.error("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        String tokenId = request.getParameter(tokenIdFieldName);
        if (tokenId == null || tokenId.isBlank()) {
            log.debug("{} Access token id extraction failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        UserProfileContext userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        userProfileContext.getAccessTokens().values()
                .forEach(accessTokens -> accessTokens.forEach(accessToken -> collectValidToken(tokenId, accessToken)));
        userProfileContext.getRefreshTokens().values().forEach(
                refreshTokens -> refreshTokens.forEach(refreshToken -> collectValidToken(tokenId, refreshToken)));
        if (token == null) {
            log.error("{} token id is not valid.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        return true;
    }

    private void collectValidToken(String tokenId, Token token) {
        if (tokenId.equals(token.getTokenId())) {
            this.token = token;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Instant now = Instant.now();
        final Instant exp = Instant.ofEpochSecond(token.getExp());
        Duration revocationLifetime;
        if (now.isAfter(exp.plus(clockSkew))) {
            log.debug("Token expiration time was in the past, returning ZERO");
            revocationLifetime = Duration.ZERO;
        } else {
            revocationLifetime = Duration.between(now, exp).plus(clockSkew);
        }
        if (revocationCache.revoke(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, token.getTokenId(),
                revocationLifetime)) {
            log.debug("{} Revoked the single token with ID '{}'", getLogPrefix(), token.getTokenId());
        } else {
            log.warn("{} Failed to revoke the single token with ID '{}'", getLogPrefix(), token.getTokenId());
            ActionSupport.buildEvent(profileRequestContext, OidcEventIds.REVOCATION_FAILED);
        }
    }
}
