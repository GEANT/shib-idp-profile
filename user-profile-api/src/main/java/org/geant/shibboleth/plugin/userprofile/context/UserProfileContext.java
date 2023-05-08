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

package org.geant.shibboleth.plugin.userprofile.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;
import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedService;
import org.geant.shibboleth.plugin.userprofile.event.api.LoginEvent;
import org.geant.shibboleth.plugin.userprofile.event.api.Token;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Context for User Profile.
 */
public final class UserProfileContext extends BaseContext {

    /** JSON based OIDC relying parties. */
    @Nullable
    private Iterable<OIDCClientInformation> oidcClientInformation;

    /** XML based relying parties. */
    @Nullable
    private Iterable<EntityDescriptor> entityDescriptors;

    /** Attribute Context per Relying Party. */
    @Nonnull
    private final Map<String, AttributeContext> rpAttributeContext = new HashMap<String, AttributeContext>();

    /** Relying Party UI Context per Relying Party. */
    @Nonnull
    private final Map<String, RelyingPartyUIContext> relyingParties = new HashMap<String, RelyingPartyUIContext>();

    /** OIDC transcodable attributes resolved for Relying Party. */
    /**
     * NOTE! TBD! Currently only OIDC transcodable attributes are shown as resolved
     * attributes for both SAML2 and OIDC clients!
     */
    @Nonnull
    private final Map<String, List<IdPAttribute>> rpEncodedJSONAttributes = new HashMap<String, List<IdPAttribute>>();

    /** access tokens generated per Relying Party.. */
    @Nonnull
    private final Map<String, List<AccessToken>> accessTokens = new HashMap<String, List<AccessToken>>();

    /** refresh tokens generated per Relying Party.. */
    @Nonnull
    private final Map<String, List<Token>> refreshTokens = new HashMap<String, List<Token>>();

    /** Connected Organizations.. */
    @Nonnull
    private final Map<String, ConnectedService> connectedOrganizations = new HashMap<String, ConnectedService>();

    /** Attributes presented as users personal data. */
    @Nonnull
    private final List<IdPAttribute> idPUserAttributes = new ArrayList<IdPAttribute>();

    /** Users login events. */
    @Nonnull
    private final List<LoginEvent> loginEvents = new ArrayList<LoginEvent>();

    /** Constructor. */
    public UserProfileContext() {
    }

    /**
     * Get Relying Party UI Context per Relying Party.
     * 
     * @return Relying Party UI Context per Relying Party
     */
    @Nonnull
    public Map<String, RelyingPartyUIContext> getRelyingParties() {
        return relyingParties;
    }

    /**
     * Get Attributes presented as users personal data.
     * 
     * @return Attributes presented as users personal data
     */
    @Nonnull
    public List<IdPAttribute> getIdPUserAttributes() {
        return idPUserAttributes;
    }

    /**
     * Get JSON based OIDC relying parties.
     * 
     * @return JSON based OIDC relying parties.
     */
    @Nullable
    public Iterable<OIDCClientInformation> getOidcClientInformation() {
        return oidcClientInformation;
    }

    /**
     * Set JSON based OIDC relying parties.
     * 
     * @param oidcClientInformation JSON based OIDC relying parties
     */
    public void setOidcClientInformation(@Nullable Iterable<OIDCClientInformation> clientInformation) {
        oidcClientInformation = clientInformation;
    }

    /**
     * Get XML based relying parties.
     * 
     * @return XML based relying parties
     */
    @Nullable
    public Iterable<EntityDescriptor> getEntityDescriptors() {
        return entityDescriptors;
    }

    /**
     * Set XML based relying parties.
     * 
     * @param descriptors XML based relying parties
     */
    public void setEntityDescriptors(@Nullable Iterable<EntityDescriptor> descriptors) {
        entityDescriptors = descriptors;
    }

    /**
     * Set Attribute Context for Relying Party.
     * 
     * @param rpId Relying Party Id
     * @param ctx  Attribute Context
     */
    public void setAttributeContext(@Nullable String rpId, @Nonnull AttributeContext ctx) {
        rpAttributeContext.put(rpId, Constraint.isNotNull(ctx, "Relying Party Attribute Context be null"));
    }

    /**
     * Get Attribute Context per Relying Party.
     * 
     * @return Attribute Context per Relying Party
     */
    @Nonnull
    public Map<String, AttributeContext> getRPAttributeContext() {
        return rpAttributeContext;
    }

    /**
     * Set resolved attribute for Relying Party.
     * 
     * @param rpId      Relying Party Id
     * @param attribute Attribute resolved
     */
    public void setEncodedJSONAttribute(@Nonnull String rpId, @Nonnull IdPAttribute attribute) {
        if (rpEncodedJSONAttributes.get(rpId) == null) {
            rpEncodedJSONAttributes.put(rpId, new ArrayList<IdPAttribute>());
        }
        rpEncodedJSONAttributes.get(Constraint.isNotNull(rpId, "Relying Party Id cannot be null"))
                .add(Constraint.isNotNull(attribute, "Attribute cannot be null"));
    }

    /**
     * Get OIDC transcodable attributes resolved for Relying Party.
     * 
     * @return OIDC transcodable attributes resolved for Relying Party
     */
    @Nonnull
    public Map<String, List<IdPAttribute>> getRPEncodedJSONAttributes() {
        return rpEncodedJSONAttributes;
    }

    /**
     * Set access token generated for Relying Party.
     * 
     * @param rpId  Relying Party Id
     * @param token as AccessToken
     */
    public void addAccessToken(@Nonnull String rpId, @Nonnull AccessToken token) {
        if (!accessTokens.containsKey(rpId)) {
            accessTokens.put(rpId, new ArrayList<AccessToken>());
        }
        accessTokens.get(rpId).add(token);
    }

    /**
     * Get access tokens generated per Relying Party.
     * 
     * @return tokens generated per Relying Party.
     */
    public @Nonnull Map<String, List<AccessToken>> getAccessTokens() {
        return accessTokens;
    }

    /**
     * Set refresh token generated for Relying Party.
     * 
     * @param rpId  Relying Party Id
     * @param token as Token
     */
    public void addRefreshToken(@Nonnull String rpId, @Nonnull Token token) {
        if (!refreshTokens.containsKey(rpId)) {
            refreshTokens.put(rpId, new ArrayList<Token>());
        }
        refreshTokens.get(rpId).add(token);
    }

    /**
     * Get refresh tokens generated per Relying Party.
     * 
     * @return tokens generated per Relying Party.
     */
    public @Nonnull Map<String, List<Token>> getRefreshTokens() {
        return refreshTokens;
    }

    /**
     * Get Connected Organizations per Relying Party.
     * 
     * @return Connected Organizations per Relying Party.
     */
    public Map<String, ConnectedService> getConnectedOrganizations() {
        return connectedOrganizations;
    }

    /**
     * Get Users login events.
     * 
     * @return users login events
     */
    public List<LoginEvent> getLoginEvents() {
        return loginEvents;
    }

}