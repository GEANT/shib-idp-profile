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
import org.opensaml.profile.context.ProfileRequestContext;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * Action resolves attributes for relying party.
 */
public class ResolveAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(ResolveAttributes.class);

    /** Service used to get the resolver for fetching attributes. */
    @Nonnull
    private final ReloadableService<AttributeResolver> attributeResolverService;

    /**
     * Strategy used to locate or create the {@link AttributeContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeContext> attributeContextCreationStrategy;

    /**
     * Strategy used to locate or create the {@link RelyingPartyContext} to
     * populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextCreationStrategy;

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Strategy used to locate or create the {@link SubjectContext} to populate. */
    @Nonnull
    private Function<ProfileRequestContext, SubjectContext> subjectContextLookupStrategy;

    /** Transcoder registry service object. */
    @NonnullAfterInit
    private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /**
     * Strategy used to locate the identity of the issuer associated with the
     * attribute resolution.
     */
    @Nullable
    private Function<ProfileRequestContext, String> issuerLookupStrategy;

    /**
     * Strategy used to locate the {@link AttributeResolutionContext} associated
     * with a given {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeResolutionContext> attributeResolutionContextLookupStrategy;

    /** For obtaining user principal name. */
    private SubjectContext subjectContext;

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    /** Context for attribute resolution . */
    AttributeResolutionContext attributeResolutionContext;

    /** Context for storing resolved attributes . */
    AttributeContext attributeCtx;

    /** Relying party context we manipulate for attribute resolving/filtering. */
    private RelyingPartyContext rpContext;

    /** Constructor. */
    ResolveAttributes(@Nonnull final ReloadableService<AttributeResolver> resolverService) {
        attributeResolverService = Constraint.isNotNull(resolverService, "AttributeResolver cannot be null");
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
        attributeResolutionContextLookupStrategy = new ChildContextLookup<>(AttributeResolutionContext.class, true);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        issuerLookupStrategy = new ResponderIdLookupFunction();
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        attributeContextCreationStrategy = new ChildContextLookup<>(AttributeContext.class, true)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingPartyContextCreationStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
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

    /**
     * Sets the strategy used to locate the {@link AttributeResolutionContext}
     * associated with a given {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the
     *                 {@link AttributeResolutionContext} associated with a given
     *                 {@link ProfileRequestContext}
     */
    public void setAttributeResolutionContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeResolutionContext> strategy) {
        attributeResolutionContextLookupStrategy = Constraint.isNotNull(strategy,
                "AttributeResolutionContext lookup strategy cannot be null");
    }

    /**
     * 
     * @param strategy
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
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
     * Set the strategy used to lookup the issuer for this attribute resolution.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate or create the {@link AttributeContext} to
     * populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setAttributeContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeContextCreationStrategy = Constraint.isNotNull(strategy,
                "AttributeContext creation strategy cannot be null");
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
        rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        attributeResolutionContext = attributeResolutionContextLookupStrategy.apply(profileRequestContext);
        if (attributeResolutionContext == null) {
            log.error("{} Unable to locate/create AttributeResolutionContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_ATTRIBUTE_CTX);
            return false;
        }
        attributeCtx = attributeContextCreationStrategy.apply(profileRequestContext);
        if (null == attributeCtx) {
            log.error("{} Unable to locate/create AttributeContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_ATTRIBUTE_CTX);
            return false;
        }
        return true;

    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        // Initialize AttributeResolutionContext
        attributeResolutionContext.setAttributeIssuerID(
                issuerLookupStrategy != null ? issuerLookupStrategy.apply(profileRequestContext) : null);
        // TODO: Set Group id to resolution context.
        attributeResolutionContext.setAttributeRecipientID(rpContext.getRelyingPartyId());
        // We set the User we resolve attributes for.
        attributeResolutionContext.setPrincipal(subjectContext.getPrincipalName());
        attributeResolutionContext.resolveAttributes(attributeResolverService);
        attributeResolutionContext.setTranscoderRegistry(transcoderRegistry);
        // Store the result.
        attributeCtx.setIdPAttributes(attributeResolutionContext.getResolvedIdPAttributes().values());
        attributeCtx.setUnfilteredIdPAttributes(attributeResolutionContext.getResolvedIdPAttributes().values());
        log.debug("{} Attributes resolved {}", getLogPrefix(), attributeCtx.getUnfilteredIdPAttributes());
        // Cleanup.
        BaseContext parent = attributeResolutionContext.getParent();
        parent.removeSubcontext(attributeResolutionContext);
        parent = attributeCtx.getParent();
        parent.removeSubcontext(attributeCtx);
        // We store the result to UserProfileContext
        userProfileContext.setAttributeContext(rpContext.getRelyingPartyId(), attributeCtx);
    }
}
