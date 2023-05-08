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

package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.impl.AttributeImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEventImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Predicates;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayDescriptionFunction;
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayNameFunction;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * Stores login information.
 */
public class UpdateLoginEvents extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(UpdateLoginEvents.class);

    /** Max entries for user login events. */
    private long maxEntries;

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
     * Strategy used to locate or create the {@link RelyingPartyContext} to
     * populate.
     */
    //TODO REMOVE
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextCreationStrategy;
    
    /**
     * Strategy used to locate the {@link RelyingPartyUIContext} associated with a given
     * {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyUIContext> relyingPartyUIContextLookupStrategy;

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

    /** Relying party id. */
    private String rpId;
    
    private RelyingPartyUIContext rpUIContext;

    @Nonnull
    private Predicate<ProfileRequestContext> collectAttributeValues;

    /** Constructor. */
    public UpdateLoginEvents() {
        super();
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
        //TODO remove
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        relyingPartyUIContextLookupStrategy = new ChildContextLookup<>(RelyingPartyUIContext.class)
                .compose(new ChildContextLookup<>(AuthenticationContext.class));
        collectAttributeValues = Predicates.alwaysFalse();
    }

    /**
     * Set max entries for user login events.
     * 
     * @param max max entries for user login events
     */
    public void setMaxEntries(long max) {
        maxEntries = max;
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
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    //TODO: remove
    public void setRelyingPartyContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingPartyContextCreationStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyUIContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyUIContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingPartyUIContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyUIContext lookup strategy cannot be null");
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

    /**
     * Predicate to decide on whether to collect attribute values.
     * 
     * @param collect predicate to decide on whether to collect attribute values.
     */
    public void setCollectAttributeValues(Predicate<ProfileRequestContext> collect) {
        collectAttributeValues = collect;
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

        RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} Unable to locate RelyingPartyContext", getLogPrefix());
            return false;
        }
        rpId = rpContext.getRelyingPartyId();
        if (rpId == null || rpId.isBlank()) {
            log.debug("{} Relying party id missing", getLogPrefix());
            return false;
        }
        
        rpUIContext = relyingPartyUIContextLookupStrategy.apply(profileRequestContext);
        if (rpUIContext == null) {
            log.debug("{} Unable to locate RelyingPartyUIContext", getLogPrefix());
            return false;
        }

        return true;
    }

    private AttributeImpl toAttributeImpl(Entry<String, IdPAttribute> entry,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        List<String> values;
        if (collectAttributeValues.test(profileRequestContext)) {
            values = new ArrayList<String>();
            entry.getValue().getValues().forEach(value -> values.add(value.getDisplayValue()));
        } else {
            values = null;
        }
        return new AttributeImpl(entry.getKey(), attributeDisplayNameFunction.apply(entry.getValue()),
                attributeDisplayDescriptionFunction.apply(entry.getValue()), values);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UsernamePrincipal user = new UsernamePrincipal(subjectContext.getPrincipalName());
        try {
            Event event = userProfileCache.getSingleEvent(user, LoginEvents.ENTRY_NAME);
            LoginEvents events = event != null ? LoginEvents.parse(event.getValue()) : new LoginEvents();
            List<AttributeImpl> attributes = new ArrayList<AttributeImpl>();
            attributeCtx.getIdPAttributes().entrySet()
                    .forEach(entry -> attributes.add(toAttributeImpl(entry, profileRequestContext)));
            LoginEventImpl loginEvent = new LoginEventImpl(rpId, rpUIContext.getServiceName(), System.currentTimeMillis() / 1000, attributes);
            events.setMaxEntries(maxEntries);
            events.getLoginEvents().add(loginEvent);
            userProfileCache.setSingleEvent(user, LoginEvents.ENTRY_NAME, events.serialize());
            log.debug("{} Updated login events {} ", getLogPrefix(), events.serialize());
        } catch (JsonProcessingException e) {
            log.error("{} Failed parsing token", getLogPrefix(), e);
            // We are intentionally not returning error.
        }

    }

}
