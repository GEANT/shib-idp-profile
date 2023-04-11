package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RefreshTokens {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens";

    private List<RefreshTokenImpl> refreshTokens = new ArrayList<RefreshTokenImpl>();

    public List<RefreshTokenImpl> getRefreshTokens() {
        return refreshTokens;
    }

    public RefreshTokens() {

    }

    public static RefreshTokens parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        RefreshTokenImpl[] refreshTokens = objectMapper.readValue(tokens, RefreshTokenImpl[].class);
        RefreshTokens refTokens = new RefreshTokens();
        refTokens.refreshTokens = new ArrayList<RefreshTokenImpl>(Arrays.asList(refreshTokens));
        return refTokens;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getRefreshTokens());

    }

}
