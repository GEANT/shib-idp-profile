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

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;

/**
 * Class for serializing OP module's OAuth2 access token information.
 */
public class AccessTokenImpl extends AbstractTokenImpl implements AccessToken {

    /** Audience of the token. */
    private final List<String> audience;

    /**
     * Constructor.
     * 
     * @param token access token
     */
    public AccessTokenImpl(AccessTokenClaimsSet token) {
        super(token);
        audience = token.getAudience();
    }

    /**
     * Constructor.
     * 
     * @param tokenId     token identifier
     * @param tokenRootId token root identifier
     * @param clientId    client identifier the token is minted for
     * @param audience    audience of the token
     * @param scope       scopes of token
     * @param exp         token expiration as seconds from epoch
     */
    @JsonCreator
    private AccessTokenImpl(@JsonProperty("tokenId") String tokenId, @JsonProperty("tokenRootId") String tokenRootId,
            @JsonProperty("clientId") String clientId, @JsonProperty("audience") List<String> audience,
            @JsonProperty("scope") List<String> scope, @JsonProperty("exp") long exp) {
        super(tokenId, tokenRootId, clientId, scope, exp);
        this.audience = audience;

    }

    /**
     * Get audience of the token.
     * 
     * @return audience of the token
     */
    public List<String> getAudience() {
        return audience;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param token json string representing the instance
     * @return AccessTokenImpl parsed from json representation
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json is not json at all
     */
    public static AccessTokenImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, AccessTokenImpl.class);
    }

}
