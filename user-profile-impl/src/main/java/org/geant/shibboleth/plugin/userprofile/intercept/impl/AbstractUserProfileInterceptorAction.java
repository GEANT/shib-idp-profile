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

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileCacheContext;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * Updates access token information in user profile cache.
 */
abstract class AbstractUserProfileInterceptorAction extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(AbstractUserProfileInterceptorAction.class);

    /**
     * Lookup strategy for user name.
     */
    @NonnullAfterInit
    protected Function<ProfileRequestContext, String> usernameLookupStrategy;

    /** User profile cache context. */
    @Nonnull
    protected UserProfileCacheContext userProfileCacheContext;

    /**
     * Lookup strategy for user profile cache context.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileCacheContext> userProfileCacheContextLookupStrategy;

    /**
     * User profile cache.
     */
    @NonnullAfterInit
    protected UserProfileCache userProfileCache;

    /** Constructor. */
    public AbstractUserProfileInterceptorAction() {
        super();
        userProfileCacheContextLookupStrategy = new ChildContextLookup<>(UserProfileCacheContext.class);
    }

    /**
     * Set Lookup strategy for user name.
     * 
     * @param strategy lookup strategy for user name
     */
    public void setUsernameLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        usernameLookupStrategy = Constraint.isNotNull(strategy, "Username lookup strategy cannot be null");
    }

    /**
     * Set Lookup strategy for user profile cache context.
     * 
     * @param strategy lookup strategy for user profile cache context
     */
    public void setUserProfileCacheContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileCacheContext> strategy) {
        checkSetterPreconditions();
        userProfileCacheContextLookupStrategy = Constraint.isNotNull(strategy,
                "UserProfileCacheContext lookup strategy cannot be null");
    }

    /**
     * Set user profile cache.
     * 
     * @param cache user profile cache
     */
    public void setUserProfileCache(@Nonnull final UserProfileCache cache) {
        checkSetterPreconditions();
        userProfileCache = Constraint.isNotNull(cache, "UserProfileCache cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (userProfileCache == null) {
            throw new ComponentInitializationException("UserProfileCache cannot be null");
        }
        if (usernameLookupStrategy == null) {
            throw new ComponentInitializationException("UsernamePrincipal lookup strategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        if (usernameLookupStrategy.apply(profileRequestContext) == null
                || usernameLookupStrategy.apply(profileRequestContext).isEmpty()) {
            log.warn("{} No username", getLogPrefix());
            return false;
        }
        userProfileCacheContext = userProfileCacheContextLookupStrategy.apply(profileRequestContext);
        if (userProfileCacheContext == null) {
            log.warn("{} No user profile cache context", getLogPrefix());
            return false;
        }
        return true;
    }
}
