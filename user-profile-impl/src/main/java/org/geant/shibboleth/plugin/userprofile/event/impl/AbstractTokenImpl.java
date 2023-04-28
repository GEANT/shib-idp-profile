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
