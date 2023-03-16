package org.geant.shibboleth.plugin.userprofile.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;
import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedOrganization;
import org.geant.shibboleth.plugin.userprofile.event.api.LoginEvent;
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

    /** tokens generated per Relying Party.. */
    @Nonnull
    private final Map<String, List<AccessToken>> rpTokens = new HashMap<String, List<AccessToken>>();

    /** Connected Organizations.. */
    @Nonnull
    private final Map<String, ConnectedOrganization> connectedOrganizations = new HashMap<String, ConnectedOrganization>();

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
     * Set token generated for Relying Party.
     * 
     * @param rpId  Relying Party Id
     * @param token as AccessToken
     */
    public void addRPToken(@Nonnull String rpId, @Nonnull AccessToken token) {
        if (!rpTokens.containsKey(rpId)) {
            rpTokens.put(rpId, new ArrayList<AccessToken>());
        }
        rpTokens.get(rpId).add(token);
    }

    /**
     * Get tokens generated per Relying Party.
     * 
     * @return tokens generated per Relying Party.
     */
    public @Nonnull Map<String, List<AccessToken>> getRPTokens() {
        return rpTokens;
    }

    /**
     * Get Connected Organizations per Relying Party.
     * 
     * @return Connected Organizations per Relying Party.
     */
    public Map<String, ConnectedOrganization> getConnectedOrganizations() {
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