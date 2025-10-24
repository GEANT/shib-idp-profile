/*
 * Copyright (c) 2022-2025, GÉANT
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class providing serialization and deserialization for refresh tokens
 * {@link RefreshTokenImpl}. Access tokens are stored to user profile storage by
 * key {@link RefreshTokens.ENTRY_NAME}
 */
public class RefreshTokens {

    /** Entry name in user profile storage. */
    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens";

    /** Refresh tokens. */
    private List<RefreshTokenImpl> refreshTokens = new ArrayList<>();

    /**
     * Constructor.
     */
    public RefreshTokens() {

    }

    /**
     * Get refresh tokens.
     * 
     * @return refresh tokens
     */
    public List<RefreshTokenImpl> getRefreshTokens() {
        return refreshTokens;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param tokens json representation.
     * @return RefreshTokens parsed from json representation.
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json is not json at all.
     */
    public static RefreshTokens parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        RefreshTokenImpl[] refreshTokens = objectMapper.readValue(tokens, RefreshTokenImpl[].class);
        RefreshTokens refTokens = new RefreshTokens();
        refTokens.refreshTokens = new ArrayList<RefreshTokenImpl>(Arrays.asList(refreshTokens));
        return refTokens;
    }

    /**
     * Serialize instance to json string.
     * 
     * @return json string representing the instance.
     * @throws JsonProcessingException something went wrong.
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getRefreshTokens());

    }
}
