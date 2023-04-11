package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.impl.AttributeImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedOrganizationImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedOrganizations;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayDescriptionFunction;
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayNameFunction;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;

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

    /** Transcoder registry service object. */
    @NonnullAfterInit
    private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /**
     * The system wide languages to inspect if there is no match between metadata
     * and browser.
     */
    @Nullable
    private List<String> fallbackLanguages;

    @NonnullAfterInit
    private AttributeDisplayDescriptionFunction attributeDisplayDescriptionFunction;

    @NonnullAfterInit
    private AttributeDisplayNameFunction attributeDisplayNameFunction;

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

    /**
     * Sets the registry of transcoding rules to apply to supply attribute display
     * metadata.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nullable final ReloadableService<AttributeTranscoderRegistry> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        transcoderRegistry = registry;
    }

    /**
     * Set the system wide default languages.
     * 
     * @param langs a semi-colon separated string.
     */
    public void setFallbackLanguages(@Nonnull @NonnullElements final List<String> langs) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        fallbackLanguages = List.copyOf(StringSupport.normalizeStringCollection(langs));
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (userProfileCache == null) {
            throw new ComponentInitializationException("UserProfileCache cannot be null");
        }
        if (transcoderRegistry == null) {
            throw new ComponentInitializationException("transcoderRegistry cannot be null");
        }
        attributeDisplayDescriptionFunction = new AttributeDisplayDescriptionFunction(getHttpServletRequest(),
                fallbackLanguages, transcoderRegistry);
        attributeDisplayNameFunction = new AttributeDisplayNameFunction(getHttpServletRequest(), fallbackLanguages,
                transcoderRegistry);
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

    private AttributeImpl toAttribute(Entry<String, IdPAttribute> entry) {
        List<String> values = new ArrayList<String>();
        entry.getValue().getValues().forEach(value -> values.add(value.getDisplayValue()));
        return new AttributeImpl(entry.getKey(), attributeDisplayNameFunction.apply(entry.getValue()),
                attributeDisplayDescriptionFunction.apply(entry.getValue()), values);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UsernamePrincipal user = new UsernamePrincipal(subjectContext.getPrincipalName());

        Event event = userProfileCache.getSingleEvent(user, ConnectedOrganizations.ENTRY_NAME);
        ConnectedOrganizations organizations;
        try {
            organizations = event != null ? ConnectedOrganizations.parse(event.getValue())
                    : new ConnectedOrganizations();
            log.debug("Connected organizations {}", organizations.serialize());
            String rpId = requesterLookupStrategy.apply(profileRequestContext);
            ConnectedOrganizationImpl organization = organizations.getConnectedOrganization().containsKey(rpId)
                    ? organizations.getConnectedOrganization().get(rpId)
                    : new ConnectedOrganizationImpl(rpId);
            organization.addCount();
            organization.getLastAttributes().clear();
            attributeCtx.getIdPAttributes().entrySet()
                    .forEach(entry -> organization.getLastAttributesImpl().add(toAttribute(entry)));
            organizations.getConnectedOrganization().put(rpId, organization);
            userProfileCache.setSingleEvent(user, ConnectedOrganizations.ENTRY_NAME, organizations.serialize());
            log.debug("{} Updated connected organizations with {} ", getLogPrefix(), organizations.serialize());
        } catch (JsonProcessingException e) {
            log.error("{} Failed parsing connected organizations", getLogPrefix(), e);
            // We are intentionally not returning error.
        }

    }

}
