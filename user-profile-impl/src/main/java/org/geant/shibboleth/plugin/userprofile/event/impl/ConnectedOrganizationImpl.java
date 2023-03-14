package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedOrganization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConnectedOrganizationImpl implements ConnectedOrganization {

    private final String rpId;
    private long times;
    private final List<String> lastAttributes;

    public ConnectedOrganizationImpl(String rpId) {
        this.rpId = rpId;
        times = 0;
        lastAttributes = new ArrayList<String>();
    }

    @JsonCreator
    private ConnectedOrganizationImpl(@JsonProperty("rpId") String rpId,
            @JsonProperty("lastAttributes") List<String> lastAttributes, @JsonProperty("times") long times) {
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

    public List<String> getLastAttributes() {
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
