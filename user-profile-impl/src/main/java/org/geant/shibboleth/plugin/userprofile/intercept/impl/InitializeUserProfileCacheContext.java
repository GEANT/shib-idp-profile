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

import javax.annotation.Nonnull;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileCacheContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.shibboleth.idp.profile.AbstractProfileAction;

/**
 * Initializes user profile cache context.
 */
public class InitializeUserProfileCacheContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(InitializeUserProfileCacheContext.class);

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final UserProfileCacheContext userProfileCacheContext = new UserProfileCacheContext();
        profileRequestContext.addSubcontext(userProfileCacheContext, true);
        log.debug("{} new UserProfileCacheContext successfully created and attached", getLogPrefix());
    }
}
