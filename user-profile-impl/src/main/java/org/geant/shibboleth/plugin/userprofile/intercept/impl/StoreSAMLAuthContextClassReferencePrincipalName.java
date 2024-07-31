/*
 * Copyright (c) 2024, GÉANT
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

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.impl.DefaultPrincipalDeterminationStrategy;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.logic.Constraint;

/**
 * Stores authentication class reference name to
 * {@link UserProfileCacheContext}.
 */
public class StoreSAMLAuthContextClassReferencePrincipalName extends AbstractUserProfileInterceptorAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(StoreSAMLAuthContextClassReferencePrincipalName.class);

    /** Strategy used to determine the AuthnContextClassRef. */
    @NonnullAfterInit
    private Function<ProfileRequestContext, AuthnContextClassRefPrincipal> classRefLookupStrategy;

    /** Constructor. */
    public StoreSAMLAuthContextClassReferencePrincipalName() {
        super();
        classRefLookupStrategy = new DefaultPrincipalDeterminationStrategy<>(AuthnContextClassRefPrincipal.class,
                new AuthnContextClassRefPrincipal(AuthnContext.UNSPECIFIED_AUTHN_CTX));
    }

    /**
     * Set the strategy function to use to obtain the authentication context class
     * reference to use.
     * 
     * @param strategy authentication context class reference lookup strategy
     */
    public void setClassRefLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthnContextClassRefPrincipal> strategy) {
        checkSetterPreconditions();
        classRefLookupStrategy = Constraint.isNotNull(strategy,
                "Authentication context class reference strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        userProfileCacheContext.setAuthnContextClassReferencePrincipalName(
                classRefLookupStrategy.apply(profileRequestContext).getName());
        log.debug("{} ACR stored to context as {}", getLogPrefix(),
                userProfileCacheContext.getAuthnContextClassReferencePrincipalName());
    }
}