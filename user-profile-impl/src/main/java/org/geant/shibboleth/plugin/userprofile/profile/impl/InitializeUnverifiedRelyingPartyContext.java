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

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.component.ComponentSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Relying party context initialization for user profile flow. There is not much
 * to do as there is no client.
 */
public class InitializeUnverifiedRelyingPartyContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(InitializeUnverifiedRelyingPartyContext.class);

    /**
     * Relying party identifier. Despite not really having such in this case some
     * authentication flow implementation rely on value existing.
     */
    @Nullable
    private String rpId = null;

    /** Strategy that will return or create a {@link RelyingPartyContext}. */
    @Nonnull
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextCreationStrategy;

    /** Constructor. */
    public InitializeUnverifiedRelyingPartyContext() {
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class, true);
    }

    /**
     * Set the strategy used to return or create the {@link RelyingPartyContext} .
     * 
     * @param strategy creation strategy
     */
    public void setRelyingPartyContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextCreationStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext creation strategy cannot be null");
    }

    /**
     * Set Relying party identifier. Despite not really having such in this case
     * some authentication flow implementation rely on value existing.
     *
     * @param identifier relying party identifier
     */
    public void setRpId(@Nullable final String identifier) {
        rpId = identifier;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate or create RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return;
        }
        rpContext.setRelyingPartyId(rpId);
        log.debug("{} new RelyingPartyContext successfully created and attached", getLogPrefix());
    }
}
