package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

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

    /** Relying Party Id we resolve attributes for. */
    private String rpId;

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    /** Relying party context we manipulate for attribute resolving/filtering. */
    private RelyingPartyContext rpContext;

    /** Context for attribute filtering . */
    private AttributeFilterContext filterContext;

    /** Constructor. */
    FilterRPAttributes(@Nonnull final ReloadableService<AttributeFilter> filterService) {
        attributeFilterService = Constraint.isNotNull(filterService, "Service cannot be null");
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
        attributeResolutionContextLookupStrategy = new ChildContextLookup<>(AttributeResolutionContext.class, true);
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
        issuerLookupStrategy = new ResponderIdLookupFunction();
        filterContextCreationStrategy = new ChildContextLookup<>(AttributeFilterContext.class, true)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
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
     * Set the strategy used to locate or create the {@link AttributeFilterContext}
     * to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setFilterContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeFilterContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

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
        rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
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
        // .setIssuerMetadataContextLookupStrategy(issuerMetadataFromFilterLookupStrategy)
        // .setRequesterMetadataContextLookupStrategy(metadataFromFilterLookupStrategy)
        // .setProxiedRequesterContextLookupStrategy(proxiesFromFilterLookupStrategy)
        // .setProxiedRequesterMetadataContextLookupStrategy(proxiedMetadataFromFilterLookupStrategy);
        filterContext.setPrefilteredIdPAttributes(
                userProfileContext.getRPAttributeContext().get(rpId).getIdPAttributes().values());
        ServiceableComponent<AttributeFilter> component = null;

        try {
            component = attributeFilterService.getServiceableComponent();
            if (null == component) {
                log.error("{} Error encountered while filtering attributes : Invalid Attribute Filter configuration",
                        getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
            } else {
                final AttributeFilter filter = component.getComponent();
                filter.filterAttributes(filterContext);
                filterContext.getParent().removeSubcontext(filterContext);
                userProfileContext.getRPAttributeContext().get(rpId)
                        .setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
                log.debug("{} Attributes filtered {}", getLogPrefix(),
                        userProfileContext.getRPAttributeContext().get(rpId).getIdPAttributes());
            }
        } catch (final AttributeFilterException e) {
            log.error("{} Error encountered while filtering attributes", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
        // Cleanup
        //BaseContext parent = filterContext.getParent();
        //parent.removeSubcontext(filterContext);
    }
}
