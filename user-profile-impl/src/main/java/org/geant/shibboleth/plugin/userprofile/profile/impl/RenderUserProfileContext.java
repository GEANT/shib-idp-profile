package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

public class RenderUserProfileContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderUserProfileContext.class);

    @NonnullAfterInit
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Constructor. */
    public RenderUserProfileContext() {
        super();
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
    }

    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        UserProfileContext userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        userProfileContext.getOidcClientInformation().forEach(client -> {
            userProfileContext.addRelyingParty(client.getID().getValue(), client.getOIDCMetadata().getName(), "OIDC");
        });
        userProfileContext.getEntityDescriptors().forEach(client -> {
            userProfileContext.addRelyingParty(client.getEntityID(), client.getEntityID(), "SAML2");
        });
        log.debug("{} Relying party information rendered {}", getLogPrefix(),
                userProfileContext.getRelyingParties().toString());

    }

}
