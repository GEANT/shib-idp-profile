package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;

public class AccessTokenImpl extends AbstractTokenImpl implements AccessToken {

    private final List<String> audience;

    public AccessTokenImpl(AccessTokenClaimsSet token) {
        super(token);
        audience = token.getAudience();
    }

    @JsonCreator
    private AccessTokenImpl(@JsonProperty("tokenId") String tokenId, @JsonProperty("tokenRootId") String tokenRootId,
            @JsonProperty("clientId") String clientId, @JsonProperty("audience") List<String> audience,
            @JsonProperty("scope") List<String> scope, @JsonProperty("exp") long exp) {
        super(tokenId, tokenRootId, clientId, scope, exp);
        this.audience = audience;

    }

    public List<String> getAudience() {
        return audience;
    }

    public static AccessTokenImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, AccessTokenImpl.class);
    }

}
