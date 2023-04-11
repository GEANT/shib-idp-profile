package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Attribute;
import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedOrganization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConnectedOrganizationImpl implements ConnectedOrganization {

    private final String rpId;
    private long times;
    private final List<AttributeImpl> lastAttributes;

    public ConnectedOrganizationImpl(String rpId) {
        this.rpId = rpId;
        times = 0;
        lastAttributes = new ArrayList<AttributeImpl>();
    }

    @JsonCreator
    private ConnectedOrganizationImpl(@JsonProperty("rpId") String rpId,
            @JsonProperty("lastAttributes") List<AttributeImpl> lastAttributes, @JsonProperty("times") long times) {
        this.rpId = rpId;
        this.lastAttributes = lastAttributes;
        this.times = times;
    }

    public String getRpId() {
        return rpId;
    }

    public long getTimes() {
        return times;
    }

    public long addCount() {
        return ++times;
    }

    public List<? extends Attribute> getLastAttributes() {
        return lastAttributes;
    }
    
    @JsonIgnore
    public List<AttributeImpl> getLastAttributesImpl() {
        return lastAttributes;
    }

    public static ConnectedOrganizationImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, ConnectedOrganizationImpl.class);
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
