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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceableComponent;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;

/**
 * Actions that filters attributes for relying party. TODO: Pass also relying
 * party metadata to filtering.
 */
public class FilterRPAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(FilterRPAttributes.class);

    /** Service used to get the engine used to filter attributes. */
    @Nonnull
    private final ReloadableService<AttributeFilter> attributeFilterService;

    /** Strategy used to locate or create the {@link AttributeFilterContext}. */
    @Nonnull
    private Function<ProfileRequestContext, AttributeFilterContext> filterContextCreationStrategy;

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

    /** Optional supplemental metadata source. */
    @Nullable
    private MetadataResolver metadataResolver;

    /**
     * Strategy used to locate the {@link AttributeResolutionContext} associated
     * with a given {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeResolutionContext> attributeResolutionContextLookupStrategy;

    /**
     * Strategy used to locate the identity of the issuer associated with the
     * attribute resolution.
     */
    @Nullable
    private Function<ProfileRequestContext, String> issuerLookupStrategy;

    /** For obtaining user principal name. */
    private SubjectContext subjectContext;

    /** Relying party identifier we resolve attributes for. */
    private String rpId;

    /** Context for user profile . */
    private UserProfileContext userProfileContext;

    /** Context for attribute filtering . */
    private AttributeFilterContext filterContext;

    /** Constructor. */
    FilterRPAttributes(@Nonnull final ReloadableService<AttributeFilter> filterService) {
        attributeFilterService = Constraint.isNotNull(filterService, "Service cannot be null");
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
        attributeResolutionContextLookupStrategy = new ChildContextLookup<>(AttributeResolutionContext.class, true);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        issuerLookupStrategy = new IssuerLookupFunction();
        filterContextCreationStrategy = new ChildContextLookup<>(AttributeFilterContext.class, true)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
    }

    /**
     * Set the strategy used to lookup the issuer for this attribute resolution.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextCreationStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
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
     * Set Lookup strategy for subject context.
     * 
     * @param strategy lookup strategy for subject context
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate or create the {@link AttributeFilterContext}
     * to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setFilterContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeFilterContext> strategy) {
        checkSetterPreconditions();
        filterContextCreationStrategy = Constraint.isNotNull(strategy,
                "AttributeContext creation strategy cannot be null");
    }

    /**
     * Set a metadata source to use during filtering.
     * 
     * @param resolver metadata resolver
     * 
     * @since 3.4.0
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        checkSetterPreconditions();
        metadataResolver = resolver;
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
        RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }
        rpId = rpContext.getRelyingPartyId();
        if (rpId == null || rpId.isBlank()) {
            log.debug("{} Relying party id extraction failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        filterContext = filterContextCreationStrategy.apply(profileRequestContext);
        if (filterContext == null) {
            log.error("{} Unable to locate or create AttributeFilterContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
            return false;
        }
        return true;

    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        filterContext.setDirection(Direction.OUTBOUND).setMetadataResolver(metadataResolver)
                .setPrincipal(subjectContext.getPrincipalName()).setAttributeRecipientID(rpId).setAttributeIssuerID(
                        issuerLookupStrategy != null ? issuerLookupStrategy.apply(profileRequestContext) : null);
        filterContext.setPrefilteredIdPAttributes(
                userProfileContext.getRPAttributeContext().get(rpId).getIdPAttributes().values());

        try {
            final ServiceableComponent<AttributeFilter> component = attributeFilterService.getServiceableComponent();
            final AttributeFilter filter = component.getComponent();
            filter.filterAttributes(filterContext);
            filterContext.getParent().removeSubcontext(filterContext);
            userProfileContext.getRPAttributeContext().get(rpId)
                    .setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
            log.debug("{} Attributes filtered {}", getLogPrefix(),
                    userProfileContext.getRPAttributeContext().get(rpId).getIdPAttributes());

        } catch (final AttributeFilterException e) {
            log.error("{} Error encountered while filtering attributes", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
        }
    }
}
