package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.plugin.oidc.op.profile.OidcEventIds;
import net.shibboleth.idp.plugin.oidc.op.storage.RevocationCacheContexts;
import net.shibboleth.idp.profile.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An action that extracts a access token id from HTTP form. The extracted
 * access token id is first verified and then to {@link UserProfileContext}.
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

    /** Message revocation cache instance to use. */
    @NonnullAfterInit
    private RevocationCache revocationCache;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Parameter name for access token id. */
    @Nonnull
    @NotEmpty
    private String accessTokenIdFieldName;

    /** For extracting user input. */
    private HttpServletRequest request;

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    /** Extracted access token. */
    private AccessToken accessToken;

    /** Constructor. */
    ExtractAndRevokeTokenFromRequest() {
        accessTokenIdFieldName = "_eventId_revokeToken";
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
     * @param cache The revocationCache to set.
     */
    public void setRevocationCache(@Nonnull final RevocationCache cache) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        revocationCache = Constraint.isNotNull(cache, "RevocationCache cannot be null");
    }

    /**
     * 
     * @param fieldName
     */
    public void setAccessTokenIdFieldName(@Nonnull @NotEmpty final String fieldName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        accessTokenIdFieldName = Constraint.isNotNull(StringSupport.trimOrNull(fieldName),
                "Access token id field name cannot be null or empty.");
    }

    /**
     * 
     * @param strategy
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (revocationCache == null) {
            throw new ComponentInitializationException("RevocationCache cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        request = getHttpServletRequest();
        if (request == null) {
            log.error("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        String accessTokenId = request.getParameter(accessTokenIdFieldName);
        if (accessTokenId == null || accessTokenId.isBlank()) {
            log.debug("{} Access token id extraction failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        userProfileContext.getRPTokens().values()
                .forEach(tokens -> tokens.forEach(accessToken -> collectValidAccessToken(accessTokenId, accessToken)));
        if (accessToken == null) {
            log.error("{} Access token id {} is not valid.", getLogPrefix(), accessTokenId);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        return true;
    }

    private void collectValidAccessToken(String accessTokenId, AccessToken accessToken) {
        if (accessTokenId.equals(accessToken.getTokenId())) {
            this.accessToken = accessToken;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Instant now = Instant.now();
        final Instant exp = Instant.ofEpochSecond(accessToken.getExp());
        Duration revocationLifetime;
        if (now.isAfter(exp.plus(clockSkew))) {
            log.debug("Token expiration time was in the past, returning ZERO");
            revocationLifetime = Duration.ZERO;
        } else {
            revocationLifetime = Duration.between(Instant.now(), Instant.ofEpochSecond(accessToken.getExp())).abs()
                    .plus(Duration.ofMinutes(5)).plus(clockSkew);
        }
        if (revocationCache.revoke(
                RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, accessToken.getTokenId(),
                revocationLifetime)) {
            log.debug("{} Revoked the single token with ID '{}'", getLogPrefix(), accessToken.getTokenId());
        } else {
            log.warn("{} Failed to revoke the single token with ID '{}'", getLogPrefix(), accessToken.getTokenId());
            ActionSupport.buildEvent(profileRequestContext, OidcEventIds.REVOCATION_FAILED);
        }
    }
}