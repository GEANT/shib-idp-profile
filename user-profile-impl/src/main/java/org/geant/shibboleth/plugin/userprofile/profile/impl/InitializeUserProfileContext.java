package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.shibboleth.oidc.metadata.ClientInformationResolver;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.core.criterion.SatisfyAnyCriterion;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Initializes User Profile Context. Context is initially populated with
 * information of all known relying parties.
 */
public class InitializeUserProfileContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(InitializeUserProfileContext.class);

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
     * OIDC Json based client information resolver.
     */
    @NonnullAfterInit
    private ClientInformationResolver clientResolver;

    /**
     * XML based metadata resolver.
     */
    /** Resolver used to look up SAML metadata. */
    @NonnullAfterInit
    private MetadataResolver metadataResolver;

    /** Constructor. */
    public InitializeUserProfileContext() {
        super();
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
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
     * Set OIDC Json based client information resolver.
     * 
     * @param resolver OIDC Json based client information resolver
     */
    public void setClientInformationResolver(@Nonnull final ClientInformationResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        clientResolver = Constraint.isNotNull(resolver, "ClientInformationResolver cannot be null");
    }

    /**
     * Set the {@link MetadataResolver} to use.
     *
     * @param resolver the resolver to use
     */
    public void setMetadataResolver(@Nonnull final MetadataResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        metadataResolver = Constraint.isNotNull(resolver, "MetadataResolver cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (userProfileCache == null) {
            throw new ComponentInitializationException("UserProfileCache cannot be null");
        }
        if (clientResolver == null) {
            throw new ComponentInitializationException("ClientInformationResolver cannot be null");
        }
        if (metadataResolver == null) {
            throw new ComponentInitializationException("RoleDescriptorResolver cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final SubjectContext subjectContext = subjectContextLookupStrategy.apply(profileRequestContext);
        if (subjectContext == null || subjectContext.getPrincipalName() == null) {
            log.error("{} No principal name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        final UserProfileContext userProfileContext = new UserProfileContext(
                userProfileCache.getRecord(new UsernamePrincipal(subjectContext.getPrincipalName())));
        try {
            // We attach XML based (for now SAML2) metadata to context.
            userProfileContext.setEntityDescriptors(metadataResolver.resolve(new CriteriaSet(new SatisfyAnyCriterion(),
                    new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME))));
            if (log.isDebugEnabled()) {
                log.debug("{} Resolved SAML SP metadata of {} entities", getLogPrefix(),
                        userProfileContext.getEntityDescriptors() instanceof Collection
                                ? ((Collection<?>) userProfileContext.getEntityDescriptors()).size()
                                : 0);
            }
            // We attach JSON based OIDC metadata to context.
            userProfileContext.setOidcClientInformation(clientResolver.resolve(new CriteriaSet()));
            if (log.isDebugEnabled()) {
                log.debug("{} Attached OIDC Json metadata of {} entities to UserProfileContext", getLogPrefix(),
                        userProfileContext.getOidcClientInformation() instanceof Collection
                                ? ((Collection<?>) userProfileContext.getOidcClientInformation()).size()
                                : 0);
            }
        } catch (ResolverException e) {
            log.warn("{} OIDC Json metadata resolving failed {}", getLogPrefix(), e);
        }
        profileRequestContext.addSubcontext(userProfileContext, true);
        log.debug("{} new UserProfileContext successfully created and attached", getLogPrefix());
    }

}
