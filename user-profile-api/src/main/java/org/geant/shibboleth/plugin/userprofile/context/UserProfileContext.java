/*
 * Copyright (c) 2022-2024, GÉANT
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

package org.geant.shibboleth.plugin.userprofile.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;
import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedService;
import org.geant.shibboleth.plugin.userprofile.event.api.LoginEvent;
import org.geant.shibboleth.plugin.userprofile.event.api.Token;
import org.geant.shibboleth.plugin.userprofile.storage.Events;
import org.geant.shibboleth.plugin.userprofile.storage.EventsCache;
import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.logic.Constraint;

/**
 * The context carrying user profile information.
 */
public final class UserProfileContext extends BaseContext implements EventsCache {

    /** Attributes presented as users personal data. */
    @Nonnull
    private final List<IdPAttribute> idPUserAttributes = new ArrayList<IdPAttribute>();

    /** Connected organizations information. */
    @Nonnull
    private final Map<String, ConnectedService> connectedOrganizations = new HashMap<String, ConnectedService>();

    /** Access tokens per relying party. */
    @Nonnull
    private final Map<String, List<AccessToken>> accessTokens = new HashMap<String, List<AccessToken>>();

    /** Refresh tokens per relying party. */
    @Nonnull
    private final Map<String, List<Token>> refreshTokens = new HashMap<String, List<Token>>();

    /** Activity page information i.e. login events. */
    @Nonnull
    private final List<LoginEvent> loginEvents = new ArrayList<LoginEvent>();

    /** Attribute context per relying party. */
    /** Note! Only used by (the most) experimental all services - page. */
    @Nonnull
    private final Map<String, AttributeContext> rpAttributeContext = new HashMap<String, AttributeContext>();

    /** Relying party ui context per relying party. */
    /** Note! Only used by (the most) experimental all services - page. */
    @Nonnull
    private final Map<String, RelyingPartyUIContext> relyingParties = new HashMap<String, RelyingPartyUIContext>();

    /** Cached user profile events. */
    private Events cachedEvents;

    /** Constructor. */
    public UserProfileContext() {
    }

    /**
     * Get connected organizations information.
     * 
     * @return connected organizations information
     */
    @Nonnull
    public List<IdPAttribute> getIdPUserAttributes() {
        return idPUserAttributes;
    }

    /**
     * Get Connected Organizations per Relying Party.
     * 
     * @return connected Organizations per Relying Party.
     */
    public Map<String, ConnectedService> getConnectedOrganizations() {
        return connectedOrganizations;
    }

    /**
     * Set access token for relying party.
     * 
     * @param rpId  relying party identifier
     * @param token access token
     */
    public void addAccessToken(@Nonnull String rpId, @Nonnull AccessToken token) {
        if (!accessTokens.containsKey(rpId)) {
            accessTokens.put(rpId, new ArrayList<AccessToken>());
        }
        accessTokens.get(rpId).add(token);
    }

    /**
     * Get access tokens per relying party.
     * 
     * @return access tokens per relying party
     */
    public @Nonnull Map<String, List<AccessToken>> getAccessTokens() {
        return accessTokens;
    }

    /**
     * Set refresh token for relying party.
     * 
     * @param rpId  relying party identifier
     * @param token refresh token
     */
    public void addRefreshToken(@Nonnull String rpId, @Nonnull Token token) {
        if (!refreshTokens.containsKey(rpId)) {
            refreshTokens.put(rpId, new ArrayList<Token>());
        }
        refreshTokens.get(rpId).add(token);
    }

    /**
     * Get refresh tokens per relying party.
     * 
     * @return refresh tokens per relying party
     */
    public @Nonnull Map<String, List<Token>> getRefreshTokens() {
        return refreshTokens;
    }

    /**
     * Get activity page information i.e. login events.
     * 
     * @return activity page information i.e. login events
     */
    public List<LoginEvent> getLoginEvents() {

        Collections.sort(loginEvents, new Comparator<LoginEvent>() {
            public int compare(LoginEvent o1, LoginEvent o2) {
                long t1 = o1.getTime();
                long t2 = o2.getTime();
                // reverse order so that most recent on top
                if (t1 > t2) {
                    return -1;
                }
                if (t2 > t1) {
                    return 1;
                }
                return 0;
            }
        });

        return loginEvents;
    }

    /**
     * Set attribute context for relying party.
     * 
     * @param rpId relying party identifier
     * @param ctx  attribute context
     */
    public void setAttributeContext(@Nullable String rpId, @Nonnull AttributeContext ctx) {
        rpAttributeContext.put(rpId, Constraint.isNotNull(ctx, "Relying party attribute context be null"));
    }

    /**
     * Get attribute contexts per relying party.
     * 
     * @return attribute contexts per relying party.
     */
    @Nonnull
    public Map<String, AttributeContext> getRPAttributeContext() {
        return rpAttributeContext;
    }

    /**
     * Get relying party ui context per relying party.
     * 
     * @return relying party ui context per relying party.
     */
    @Nonnull
    public Map<String, RelyingPartyUIContext> getRelyingParties() {
        return relyingParties;
    }

    /** {@inheritDoc} */
    @Override
    public void setEvents(Events events) {
        cachedEvents = events;
    }

    /** {@inheritDoc} */
    @Override
    public Events getEvents() {
        return cachedEvents;
    }

}
