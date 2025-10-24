/*
 * Copyright (c) 2022-2025, GÉANT
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

import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;
import org.opensaml.profile.context.ProfileRequestContext;
import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Action sets the 'personal data' attributes of user to
 * {@link UserProfileContext}.
 */
public class RenderIdPAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(RenderIdPAttributes.class);

    /**
     * Strategy used to locate or create the {@link UserProfileContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, UserProfileContext> userProfileContextLookupStrategy;

    /** Context for user profile . */
    private UserProfileContext userProfileContext;

    /** Attribute identifiers of 'personal data' attributes. */
    private Collection<String> idPAttributes;

    /** Constructor. */
    RenderIdPAttributes() {
        userProfileContextLookupStrategy = new ChildContextLookup<>(UserProfileContext.class);
    }

    /**
     * Set strategy used to locate or create the {@link UserProfileContext} to
     * populate.
     * 
     * @param strategy Strategy used to locate or create the
     *                 {@link UserProfileContext} to populate.
     */
    public void setUserProfileContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, UserProfileContext> strategy) {
        checkSetterPreconditions();
        Constraint.isNotNull(strategy, "UserProfileContext lookup strategy cannot be null");
        userProfileContextLookupStrategy = strategy;
    }

    /**
     * Set attribute identifiers of 'personal data' attributes.
     * 
     * @param attributes attribute identifiers of 'personal data' attributes
     */
    public void setIdPUserAttributes(@Nonnull @NonnullElements final Collection<String> attributes) {
        assert attributes != null;
        idPAttributes = StringSupport.normalizeStringCollection(attributes);
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        if (idPAttributes == null || idPAttributes.isEmpty()) {
            // Nothing to do, no selected attributes.
            return false;
        }
        userProfileContext = userProfileContextLookupStrategy.apply(profileRequestContext);
        if (userProfileContext == null) {
            log.error("{} No UserProfileContext name available.", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        return userProfileContext.getRPAttributeContext().get(null) != null;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        userProfileContext.getIdPUserAttributes().clear();
        for (final IdPAttribute attribute : userProfileContext.getRPAttributeContext().get(null).getIdPAttributes()
                .values()) {
            if (attribute != null && !attribute.getValues().isEmpty() && idPAttributes.contains(attribute.getId())) {
                userProfileContext.getIdPUserAttributes().add(attribute);
                if (log.isDebugEnabled()) {
                    log.debug("{} Adding attribute {}", getLogPrefix(), attribute);
                }
            }
        }
    }
}
