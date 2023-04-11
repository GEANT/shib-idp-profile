package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.RefreshTokenClaimsSet;

public class RefreshTokenImpl extends AbstractTokenImpl implements Token {

    public RefreshTokenImpl(RefreshTokenClaimsSet token) {
        super(token);
    }

    @JsonCreator
    private RefreshTokenImpl(@JsonProperty("tokenId") String tokenId, @JsonProperty("tokenRootId") String tokenRootId,
            @JsonProperty("clientId") String clientId, @JsonProperty("scope") List<String> scope,
            @JsonProperty("exp") long exp) {
        super(tokenId, tokenRootId, clientId, scope, exp);
    }

    public static RefreshTokenImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, RefreshTokenImpl.class);
    }

}
