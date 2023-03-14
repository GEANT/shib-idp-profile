package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.text.ParseException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokenImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.AccessTokenContext;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;
import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

/**
 * Stores Access Token information if available.
 * 
 * TODO TESTS TODO JWT ACCESS TOKEN TODO Whether to execution to be
 * configurable.
 */
public class StoreToken extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(StoreToken.class);

    /** Data sealer for unwrapping authorization code. */
    @NonnullAfterInit
    private DataSealer dataSealer;

    /** Strategy used to locate the subcontext with the token. */
    @Nonnull
    private Function<ProfileRequestContext, AccessTokenContext> accessTokenContextLookupStrategy;

    /** Token context. */
    @Nullable
    private AccessTokenContext tokenCtx;

    /** Subject context. */
    @Nonnull
    private SubjectContext subjectContext;

    /**
     * Lookup strategy for Subject Context.
     */
    @Nonnull
    private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;

    /**
     * User Profile Cache.
     */
    @NonnullAfterInit
    private UserProfileCache userProfileCache;

    /** Constructor. */
    public StoreToken() {
        super();
        accessTokenContextLookupStrategy = new ChildContextLookup<>(AccessTokenContext.class)
                .compose(new ChildContextLookup<>(OIDCAuthenticationResponseContext.class)
                        .compose(new OutboundMessageContextLookup()));
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }

    /**
     * Set the strategy used to lookup the {@link AccessTokenContext} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setAccessTokenContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AccessTokenContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        accessTokenContextLookupStrategy = Constraint.isNotNull(strategy,
                "AccessTokenContext lookup strategy cannot be null");
    }

    /**
     * Set the data sealer instance to use.
     * 
     * @param sealer sealer to use
     */
    public void setDataSealer(@Nonnull final DataSealer sealer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        dataSealer = Constraint.isNotNull(sealer, "DataSealer cannot be null");
    }

    /**
     * Set Lookup strategy for Subject Context.
     * 
     * @param strategy Lookup strategy for Subject Context
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Set User Profile Cache.
     * 
     * @param cache User Profile Cache
     */
    public void setUserProfileCache(@Nonnull final UserProfileCache cache) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        userProfileCache = Constraint.isNotNull(cache, "UserProfileCache cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (userProfileCache == null) {
            throw new ComponentInitializationException("UserProfileCache cannot be null");
        }
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

        subjectContext = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectContext == null || subjectContext.getPrincipalName() == null) {
            log.error("{} No principal name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UsernamePrincipal user = new UsernamePrincipal(subjectContext.getPrincipalName());
        try {
            JSONObject entry = userProfileCache.getSingleEvent(user, AccessTokens.ENTRY_NAME);
            AccessTokens tokens = entry != null ? AccessTokens.parse(((String) entry.get("value")))
                    : new AccessTokens();
            tokens.getAccessTokens().removeIf(accessToken -> accessToken.getExp() < System.currentTimeMillis() / 1000);
            AccessTokenImpl token = new AccessTokenImpl(AccessTokenClaimsSet.parse(tokenCtx.getOpaque(), dataSealer));
            tokens.getAccessTokens().add(token);
            userProfileCache.setSingleEvent(user, AccessTokens.ENTRY_NAME, tokens.serialize());
            log.debug("{} Updated access tokens {} ", getLogPrefix(), tokens.serialize());
        } catch (JsonProcessingException | ParseException | DataSealerException e) {
            log.error("{} Failed parsing token", getLogPrefix(), e);
            // We are intentionally not returning error.
        }

    }

}
