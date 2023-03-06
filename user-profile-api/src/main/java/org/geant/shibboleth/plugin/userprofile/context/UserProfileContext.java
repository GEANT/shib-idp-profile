package org.geant.shibboleth.plugin.userprofile.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Context for User Profile.
 */
public final class UserProfileContext extends BaseContext {

    /** The user record. */
    @Nonnull
    private final JSONObject userRecord;

    /** The Relying Parties. */
    @Nonnull
    private final JSONObject relyingParties = new JSONObject();

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
    private final Map<String, RelyingPartyUIContext> rpRelyingPartyUIContext = new HashMap<String, RelyingPartyUIContext>();

    /** OIDC transcodable attributes resolved for Relying Party. */
    /**
     * NOTE! TBD! Currently only OIDC transcodable attributes are shown as resolved
     * attributes for both SAML2 and OIDC clients! NOTE! TBD! Actual encoded
     * JSONObject value is not really used. Remove it!
     */
    @Nonnull
    private final Map<String, Map<IdPAttribute, JSONObject>> rpEncodedJSONAttributes = new HashMap<String, Map<IdPAttribute, JSONObject>>();

    /** tokens generated per Relying Party.. */
    @Nonnull
    private final Map<String, List<JSONObject>> rpTokens = new HashMap<String, List<JSONObject>>();

    /** Attributes presented as users personal data. */
    @Nonnull
    private final List<IdPAttribute> idPUserAttributes = new ArrayList<IdPAttribute>();

    /** Constructor. */
    public UserProfileContext(@Nullable JSONObject record) {
        userRecord = record;
    }

    /**
     * Get Relying Party UI Context per Relying Party.
     * 
     * @return Relying Party UI Context per Relying Party
     */
    @Nonnull
    public Map<String, RelyingPartyUIContext> getRPRelyingPartyUIContextes() {
        return rpRelyingPartyUIContext;
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
     * Get The user record.
     * 
     * @return The user record
     */
    @Nonnull
    public JSONObject getRecord() {
        return userRecord;
    }

    /**
     * Get The Relying Parties.
     * 
     * @return The Relying Parties.
     */
    @Nonnull
    public JSONObject getRelyingParties() {
        return relyingParties;
    }

    /**
     * Add Relying Party.
     * 
     * @param rpId Relying Party Id
     * @param name Relying Party Name
     * @param type Relying Party Type
     */
    public void addRelyingParty(@Nonnull String rpId, @Nonnull String name, @Nonnull String type) {
        JSONObject content = new JSONObject();
        content.put("name", Constraint.isNotNull(name, "Relying Party name cannot be null"));
        content.put("type", Constraint.isNotNull(type, "Relying Party type cannot be null"));
        relyingParties.put(Constraint.isNotNull(rpId, "Relying Party Id cannot be null"), content);
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
     * @param encoded   Attribute encoded
     */
    public void setEncodedJSONAttribute(@Nonnull String rpId, @Nonnull IdPAttribute attribute,
            @Nonnull JSONObject encoded) {
        if (rpEncodedJSONAttributes.get(rpId) == null) {
            rpEncodedJSONAttributes.put(rpId, new HashMap<IdPAttribute, JSONObject>());
        }
        rpEncodedJSONAttributes.get(Constraint.isNotNull(rpId, "Relying Party Id cannot be null")).put(
                Constraint.isNotNull(attribute, "Attribute cannot be null"),
                Constraint.isNotNull(encoded, "Encoded Attribute cannot be null"));
    }

    /**
     * Get OIDC transcodable attributes resolved for Relying Party.
     * 
     * @return OIDC transcodable attributes resolved for Relying Party
     */
    @Nonnull
    public Map<String, Map<IdPAttribute, JSONObject>> getRPEncodedJSONAttributes() {
        return rpEncodedJSONAttributes;
    }

    /**
     * Set token generated for Relying Party.
     * 
     * @param rpId  Relying Party Id
     * @param token as JSONObject
     */
    public void addRPToken(@Nonnull String rpId, @Nonnull JSONObject token) {
        if (!rpTokens.containsKey(rpId)) {
            rpTokens.put(rpId, new ArrayList<JSONObject>());
        }
        rpTokens.get(rpId).add(token);
    }

    /**
     * Get tokens generated per Relying Party.
     * 
     * @return tokens generated per Relying Party.
     */
    public @Nonnull Map<String, List<JSONObject>> getRPTokens() {
        return rpTokens;
    }

}