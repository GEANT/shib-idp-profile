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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AccessTokens {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens";

    private List<AccessTokenImpl> accessTokens = new ArrayList<AccessTokenImpl>();

    public List<AccessTokenImpl> getAccessTokens() {
        return accessTokens;
    }

    public AccessTokens() {

    }

    public static AccessTokens parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        AccessTokenImpl[] accessTokens = objectMapper.readValue(tokens, AccessTokenImpl[].class);
        AccessTokens accTokens = new AccessTokens();
        accTokens.accessTokens = new ArrayList<AccessTokenImpl>(Arrays.asList(accessTokens));
        return accTokens;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getAccessTokens());

    }

}
