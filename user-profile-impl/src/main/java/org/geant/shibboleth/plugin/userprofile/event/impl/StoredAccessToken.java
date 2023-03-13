package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;

public class StoredAccessToken implements AccessToken{

    private final String tokenId;
    private final String tokenRootId;
    private final String clientId;
    private final List<String> audience;
    private final List<String> scope;
    private final long exp;

    public StoredAccessToken(AccessTokenClaimsSet token) {
        tokenId = token.getID();
        tokenRootId = token.getRootTokenIdentifier();
        clientId = token.getClientID().getValue();
        audience = token.getAudience();
        scope = token.getScope().toStringList();
        exp = token.getExp().getEpochSecond();
    }

    @JsonCreator
    private StoredAccessToken(@JsonProperty("tokenId") String tokenId, @JsonProperty("tokenRootId") String tokenRootId,
            @JsonProperty("clientId") String clientId, @JsonProperty("audience") List<String> audience,
            @JsonProperty("scope") List<String> scope, @JsonProperty("exp") long exp) {
        this.tokenId = tokenId;
        this.tokenRootId = tokenRootId;
        this.clientId = clientId;
        this.audience = audience;
        this.scope = scope;
        this.exp = exp;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getTokenRootId() {
        return tokenRootId;
    }

    public String getClientId() {
        return clientId;
    }

    public List<String> getAudience() {
        return audience;
    }

    public List<String> getScope() {
        return scope;
    }

    public long getExp() {
        return exp;
    }
    
    public static StoredAccessToken parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, StoredAccessToken.class);
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
