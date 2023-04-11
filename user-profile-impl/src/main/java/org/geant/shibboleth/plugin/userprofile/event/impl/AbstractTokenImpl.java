package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.TokenClaimsSet;

public abstract class AbstractTokenImpl implements Token {

    private final String tokenId;
    private final String tokenRootId;
    private final String clientId;
    private final List<String> scope;
    private final long exp;

    protected AbstractTokenImpl(TokenClaimsSet token) {
        tokenId = token.getID();
        tokenRootId = token.getRootTokenIdentifier();
        clientId = token.getClientID().getValue();
        scope = token.getScope().toStringList();
        exp = token.getExp().getEpochSecond();
    }

    protected AbstractTokenImpl(String tokenId, String tokenRootId, String clientId, List<String> scope, long exp) {
        this.tokenId = tokenId;
        this.tokenRootId = tokenRootId;
        this.clientId = clientId;
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

    public List<String> getScope() {
        return scope;
    }

    public long getExp() {
        return exp;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
