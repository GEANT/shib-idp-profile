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
import org.opensaml.profile.context.ProfileRequestContext;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An action that extracts a relying party id from HTTP form. The extracted
 * relying party id is first verified to exist and then set to relying party
 * context.
 */

public class ExtractRelyingPartyIdFromRequest extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(ExtractRelyingPartyIdFromRequest.class);

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

    /** Parameter name for relying party identifier. */
    @Nonnull
    @NotEmpty
    private String rpIdFieldName;

    /** For extracting user input. */
    private HttpServletRequest request;

    /** Relying party identifier we resolve attributes for. */
    private String rpId;

    /** context for user profile . */
    private UserProfileContext userProfileContext;

    /** Constructor. */
    ExtractRelyingPartyIdFromRequest() {
        rpIdFieldName = "_eventId_showAttributes";
        relyingPartyContextCreationStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
    }

    /**
     * Set parameter name for relying party identifier.
     * 
     * @param fieldName parameter name for relying party identifier
     */
    public void setRpIdFieldName(@Nonnull @NotEmpty final String fieldName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        rpIdFieldName = Constraint.isNotNull(StringSupport.trimOrNull(fieldName),
                "Relying Party field name cannot be null or empty.");
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
     * Set strategy used to locate or create the {@link UserProfileContext} to
     * populate.
     * 
     * @param strategy strategy used to locate the {@link UserProfileContext} to
     *                 populate
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        request = getHttpServletRequest();
        if (request == null) {
            log.error("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        rpId = request.getParameter(rpIdFieldName);
        if (rpId == null || rpId.isBlank()) {
            log.debug("{} Relying party id extraction failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        if (!userProfileContext.getRelyingParties().containsKey(rpId)) {
            log.error("{} Relying Party {} is unknown.", getLogPrefix(), rpId);
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        return true;

    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return;
        }
        rpContext.setRelyingPartyId(rpId);

    }
}
