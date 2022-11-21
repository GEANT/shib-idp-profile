package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import org.opensaml.profile.context.ProfileRequestContext;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

public class RenderIdPAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderIdPAttributes.class);

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Context for User Profile . */
    private UserProfileContext userProfileContext;

    private Collection<String> idPAttributes;

    /** Constructor. */
    RenderIdPAttributes() {
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
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

    public void setIdPUserAttributes(@Nonnull @NonnullElements final Collection<String> attributes) {
        if (attributes != null) {
            idPAttributes = StringSupport.normalizeStringCollection(attributes);
        }
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        if (idPAttributes == null || idPAttributes.isEmpty()) {
            // Nothing to do
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
        for (final IdPAttribute attribute : userProfileContext.getRPAttributeContext().get(null).getIdPAttributes()
                .values()) {
            if (attribute != null && !attribute.getValues().isEmpty() && idPAttributes.contains(attribute.getId())) {
                userProfileContext.getIdPUserAttributes().add(attribute);
                log.debug("{} Adding attribute {}", getLogPrefix(), attribute.toString());
            }
        }
    }
}