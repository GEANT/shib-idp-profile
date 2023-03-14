package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConnectedOrganizations {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedOrganizations";

    private Map<String, ConnectedOrganizationImpl> connectedOrganizations = new HashMap<String, ConnectedOrganizationImpl>();

    public Map<String, ConnectedOrganizationImpl> getConnectedOrganization() {
        return connectedOrganizations;
    }

    public ConnectedOrganizations() {

    }

    public static ConnectedOrganizations parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, ConnectedOrganizationImpl>> typeRef = new TypeReference<HashMap<String, ConnectedOrganizationImpl>>() {
        };
        Map<String, ConnectedOrganizationImpl> accessTokens = objectMapper.readValue(tokens, typeRef);
        ConnectedOrganizations accTokens = new ConnectedOrganizations();
        accTokens.getConnectedOrganization().putAll(accessTokens);
        return accTokens;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getConnectedOrganization());

    }

}
