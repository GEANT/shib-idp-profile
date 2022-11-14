package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.config.navigate.ForceAuthnProfileConfigPredicate;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);

    /** Extracts forceAuthn property from profile config. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;

    /**
     * Constructor.
     */
    public InitializeAuthenticationContext() {
        forceAuthnPredicate = new ForceAuthnProfileConfigPredicate();
    }

    /**
     * Set the predicate to apply to derive the message-independent forced authn default. 
     * 
     * @param condition condition to set
     * 
     * @since 3.1.0
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        log.debug("{} Initializing authentication context", getLogPrefix());
        final AuthenticationContext authnCtx = new AuthenticationContext();
        authnCtx.setForceAuthn(forceAuthnPredicate.test(profileRequestContext));
        profileRequestContext.addSubcontext(authnCtx, true);
        log.debug("{} Created authentication context: {}", getLogPrefix(), authnCtx);
    }

}