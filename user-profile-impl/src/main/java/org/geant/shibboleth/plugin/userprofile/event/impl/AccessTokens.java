package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AccessTokens {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens";

    private List<StoredAccessToken> accessTokens = new ArrayList<StoredAccessToken>();

    public List<StoredAccessToken> getAccessTokens() {
        return accessTokens;
    }

    public AccessTokens() {

    }

    public static AccessTokens parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StoredAccessToken[] accessTokens = objectMapper.readValue(tokens, StoredAccessToken[].class);
        AccessTokens accTokens = new AccessTokens();
        accTokens.accessTokens = new ArrayList<StoredAccessToken>(Arrays.asList(accessTokens));
        return accTokens;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getAccessTokens());

    }

}
