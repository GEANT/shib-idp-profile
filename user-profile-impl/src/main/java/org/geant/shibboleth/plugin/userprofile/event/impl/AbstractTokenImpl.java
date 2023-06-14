/*
 * Copyright (c) 2022-2023, GÉANT
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.TokenClaimsSet;

/**
 * Abstract class for classes serializing OP module's OAuth2 token information.
 */
public abstract class AbstractTokenImpl implements Token {

    /** Token identifier. */
    private final String tokenId;

    /** Token root identifier. */
    private final String tokenRootId;

    /** Client identifier the token is minted for. */
    private final String clientId;

    /** Scopes of token. */
    private final List<String> scope;

    /** Token expiration as seconds from epoch. */
    private final long exp;

    /**
     * Constructor.
     * 
     * @param token token
     */
    protected AbstractTokenImpl(TokenClaimsSet token) {
        tokenId = token.getID();
        tokenRootId = token.getRootTokenIdentifier();
        clientId = token.getClientID().getValue();
        scope = token.getScope().toStringList();
        exp = token.getExp().getEpochSecond();
    }

    /**
     * Constructor.
     * 
     * @param tokenId     token identifier
     * @param tokenRootId token root identifier
     * @param clientId    client identifier the token is minted for
     * @param scope       scopes of token
     * @param exp         token expiration as seconds from epoch
     */
    protected AbstractTokenImpl(String tokenId, String tokenRootId, String clientId, List<String> scope, long exp) {
        this.tokenId = tokenId;
        this.tokenRootId = tokenRootId;
        this.clientId = clientId;
        this.scope = scope;
        this.exp = exp;
    }

    /**
     * Get token identifier.
     * 
     * @return token identifier
     */
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Get token root identifier.
     * 
     * @return token root identifier
     */
    public String getTokenRootId() {
        return tokenRootId;
    }

    /**
     * Get client identifier the token is minted for.
     * 
     * @return client identifier the token is minted for
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get scopes of token.
     * 
     * @return scopes of token
     */
    public List<String> getScope() {
        return scope;
    }

    /**
     * Get token expiration as seconds from epoch.
     * 
     * @return token expiration as seconds from epoch
     */
    public long getExp() {
        return exp;
    }

    /**
     * Get json representation of the token
     * 
     * @return json representation of the token
     * @throws JsonProcessingException if error occurs
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
