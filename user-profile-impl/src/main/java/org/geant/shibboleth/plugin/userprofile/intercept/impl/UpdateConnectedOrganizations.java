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
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServiceImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServices;
import org.geant.shibboleth.plugin.userprofile.storage.Event;
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
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayDescriptionFunction;
import net.shibboleth.idp.consent.logic.impl.AttributeDisplayNameFunction;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.component.ComponentSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Updates connected organizations data in user profile cache.
 */
public class UpdateConnectedOrganizations extends AbstractUserProfileInterceptorAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(UpdateConnectedOrganizations.class);

    /** Function used to obtain the requester identifier. */
    @Nullable
    private Function<ProfileRequestContext, String> requesterLookupStrategy;

    /**
     * Strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /** AttributeContext to use. */
    @Nullable
    private AttributeContext attributeCtx;

    /** Transcoder registry service object. */
    @NonnullAfterInit
    private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /**
     * The system wide languages to inspect if there is no match between metadata
     * and browser.
     */
    @Nullable
    private List<String> fallbackLanguages;

    /** Function to resolve attribute description. */
    @NonnullAfterInit
    private AttributeDisplayDescriptionFunction attributeDisplayDescriptionFunction;

    /** Function to resolve attribute name. */
    @NonnullAfterInit
    private AttributeDisplayNameFunction attributeDisplayNameFunction;

    /** Whether to collect attribute values. */
    @Nonnull
    private Predicate<ProfileRequestContext> collectAttributeValues;

    /**
     * Strategy used to locate the {@link RelyingPartyUIContext} associated with a
     * given {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyUIContext> relyingPartyUIContextLookupStrategy;

    /** Relying party ui context. */
    private RelyingPartyUIContext rpUIContext;

    /** Constructor. */
    public UpdateConnectedOrganizations() {
        super();
        requesterLookupStrategy = new RelyingPartyIdLookupFunction();
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
        relyingPartyUIContextLookupStrategy = new ChildContextLookup<>(RelyingPartyUIContext.class)
                .compose(new ChildContextLookup<>(AuthenticationContext.class));
        collectAttributeValues = Predicates.alwaysFalse();
    }

    /**
     * Set the strategy used to locate the requester identifier.
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

    /**
     * Predicate to decide on whether to collect attribute values.
     * 
     * @param collect predicate to decide on whether to collect attribute values.
     */
    public void setCollectAttributeValues(Predicate<ProfileRequestContext> collect) {
        collectAttributeValues = collect;
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

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
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

        attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.debug("{} No attribute context available, nothing to do", getLogPrefix());
            return false;
        }

        rpUIContext = relyingPartyUIContextLookupStrategy.apply(profileRequestContext);
        if (rpUIContext == null) {
            log.debug("{} Unable to locate relying party ui context", getLogPrefix());
            return false;
        }

        return true;
    }

    /**
     * Transform IdPAttribute to AttributeImpl.
     * 
     * @param entry                 IdPAttribute to be transformed
     * @param profileRequestContext current profile context
     * @return AttributeImpl to be stored to user profile cache.
     */
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
        String user = usernameLookupStrategy.apply(profileRequestContext);
        Event event = userProfileCache.getSingleEvent(user, ConnectedServices.ENTRY_NAME, userProfileCacheContext);
        ConnectedServices organizations;
        try {
            organizations = event != null ? ConnectedServices.parse(event.getValue()) : new ConnectedServices();
            log.debug("Connected organizations {}", organizations.serialize());
            String rpId = requesterLookupStrategy.apply(profileRequestContext);
            ConnectedServiceImpl organization = organizations.getConnectedServices().containsKey(rpId)
                    ? organizations.getConnectedServices().get(rpId)
                    : new ConnectedServiceImpl(rpId, rpUIContext.getServiceName());
            organization.addCount();
            organization.getLastAttributes().clear();
            attributeCtx.getIdPAttributes().entrySet().forEach(
                    entry -> organization.getLastAttributesImpl().add(toAttributeImpl(entry, profileRequestContext)));
            organizations.getConnectedServices().put(rpId, organization);
            userProfileCache.setSingleEvent(ConnectedServices.ENTRY_NAME, organizations.serialize(),
                    userProfileCacheContext);
            log.debug("{} Updated connected organizations with {} ", getLogPrefix(), organizations.serialize());
        } catch (JsonProcessingException e) {
            log.error("{} Failed parsing connected organizations", getLogPrefix(), e);
            // We are intentionally not returning error.
        }
    }

}
