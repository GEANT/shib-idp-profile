package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Updates called connected organizations data.
 * 
 * TODO Execute only in Authenticating endpoint. TODO TESTS TODO Whether to
 * execution to be configurable.
 */
public class UpdateConnectedOrganizations extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(UpdateConnectedOrganizations.class);

    /** Function used to obtain the requester ID. */
    @Nullable
    private Function<ProfileRequestContext, String> requesterLookupStrategy;

    /** Subject context. */
    @Nonnull
    private SubjectContext subjectContext;

    /**
     * Lookup strategy for Subject Context.
     */
    @Nonnull
    private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;

    /**
     * Strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /** AttributeContext to use. */
    @Nullable
    private AttributeContext attributeCtx;

    /**
     * User Profile Cache.
     */
    @NonnullAfterInit
    private UserProfileCache userProfileCache;

    /** Constructor. */
    public UpdateConnectedOrganizations() {
        super();
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
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
     * Set the strategy used to locate the requester ID.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterLookupStrategy(@Nullable final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requesterLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with
     * a given {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext}
     *                 associated with a given {@link ProfileRequestContext}
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeContext lookup strategy cannot be null");
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
            log.debug("{} No principal name available.", getLogPrefix());
            return false;
        }

        attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.debug("{} No AttributeSubcontext available, nothing to do", getLogPrefix());
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UsernamePrincipal user = new UsernamePrincipal(subjectContext.getPrincipalName());
        JSONObject entry = userProfileCache.getSingleEvent(user, "CONNECTED_ORGANIZATIONS");
        JSONObject connectedOrganizations = entry != null ? (JSONObject) entry.get("value") : new JSONObject();
        log.debug("Connected Organizations {}", connectedOrganizations.toString());
        String rpId = requesterLookupStrategy.apply(profileRequestContext);
        JSONObject connectedOrganization = (connectedOrganizations.get(rpId) instanceof JSONObject)
                ? (JSONObject) connectedOrganizations.get(rpId)
                : new JSONObject().appendField("times", 0);
        connectedOrganization.put("times", connectedOrganization.getAsNumber("times").longValue() + 1);
        JSONArray attributes = new JSONArray();
        attributeCtx.getIdPAttributes().keySet().forEach(attributeId -> attributes.add(attributeId));
        connectedOrganization.put("attributes", attributes);
        log.debug("{} Updated connected organization {} {} ", getLogPrefix(), rpId, connectedOrganization.toString());
        userProfileCache.setSingleEvent(user, "CONNECTED_ORGANIZATIONS", connectedOrganizations);

    }

}
