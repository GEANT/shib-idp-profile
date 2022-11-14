package org.geant.shibboleth.plugin.userprofile.context;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import com.google.gson.JsonObject;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

import net.minidev.json.JSONObject;
import net.shibboleth.idp.attribute.context.AttributeContext;

public final class UserProfileContext extends BaseContext {

    private final JsonObject record;

    private final JsonObject relyingParties;

    private Iterable<OIDCClientInformation> oidcClientInformation;
    private Iterable<EntityDescriptor> entityDescriptors;

    private Map<String, AttributeContext> rpAttributeContext = new HashMap<String, AttributeContext>();
    private Map<String, Map<String, JSONObject>> rpEncodedJSONAttributes = new HashMap<String, Map<String, JSONObject>>();

    /** Constructor. */
    public UserProfileContext(JsonObject record) {
        this.record = record;
        relyingParties = new JsonObject();
    }

    public Iterable<OIDCClientInformation> getOidcClientInformation() {
        return oidcClientInformation;
    }

    public void setOidcClientInformation(Iterable<OIDCClientInformation> oidcClientInformation) {
        this.oidcClientInformation = oidcClientInformation;
    }

    public Iterable<EntityDescriptor> getEntityDescriptors() {
        return entityDescriptors;
    }

    public void setEntityDescriptors(Iterable<EntityDescriptor> descriptors) {
        entityDescriptors = descriptors;
    }

    public JsonObject getRecord() {
        return record;
    }

    public JsonObject getRelyingParties() {
        return relyingParties;
    }

    public void addRelyingParty(String rpId, String name, String type) {
        JsonObject content = new JsonObject();
        content.addProperty("name", name);
        content.addProperty("type", type);
        relyingParties.add(rpId, content);
    }

    public void setAttributeContext(String rpId, AttributeContext ctx) {
        rpAttributeContext.put(rpId, ctx);
    }

    public Map<String, AttributeContext> getRPAttributeContext() {
        return rpAttributeContext;
    }

    public void setEncodedJSONAttribute(String rpId, String attributeId, JSONObject attribute) {
        if (rpEncodedJSONAttributes.get(rpId) == null) {
            rpEncodedJSONAttributes.put(rpId, new HashMap<String, JSONObject>());
        }
        rpEncodedJSONAttributes.get(rpId).put(attributeId, attribute);
    }

    public Map<String, Map<String, JSONObject>> getRPEncodedJSONAttributes() {
        return rpEncodedJSONAttributes;
    }

}