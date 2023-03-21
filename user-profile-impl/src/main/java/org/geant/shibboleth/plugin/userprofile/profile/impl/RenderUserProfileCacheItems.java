package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedOrganizations;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Renders information to user profile context.
 * 
 * TODO TESTS
 */
public class RenderUserProfileCacheItems extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderUserProfileCacheItems.class);

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

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Constructor. */
    public RenderUserProfileCacheItems() {
        super();
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
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

        subjectContext = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectContext == null || subjectContext.getPrincipalName() == null) {
            log.error("{} No principal name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UsernamePrincipal user = new UsernamePrincipal(subjectContext.getPrincipalName());
        Event event = userProfileCache.getSingleEvent(user, AccessTokens.ENTRY_NAME);
        try {
            AccessTokens tokens = event != null ? AccessTokens.parse(event.getValue()) : new AccessTokens();
            // TODO: take clockSkew into consideration
            tokens.getAccessTokens().removeIf(accessToken -> accessToken.getExp() < System.currentTimeMillis() / 1000);
            userProfileCache.setSingleEvent(user, AccessTokens.ENTRY_NAME, tokens.serialize());
            log.debug("{} Updated access tokens {} ", getLogPrefix(), tokens.serialize());
            // TODO: Now remove all revoked tokens from tokens displayed.
            tokens.getAccessTokens()
                    .forEach((accessToken -> userProfileContext.addRPToken(accessToken.getClientId(), accessToken)));
        } catch (JsonProcessingException e) {
            log.error("{} Failed processing access tokens.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        event = userProfileCache.getSingleEvent(user, ConnectedOrganizations.ENTRY_NAME);
        try {
            ConnectedOrganizations organizations = event != null ? ConnectedOrganizations.parse(event.getValue())
                    : new ConnectedOrganizations();
            organizations.getConnectedOrganization().forEach((rpId, connectedOrganization) -> userProfileContext
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
