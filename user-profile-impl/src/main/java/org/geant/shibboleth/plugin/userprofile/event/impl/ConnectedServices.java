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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class providing serialization and deserialization for map of 'relying party'
 * -> {@link ConnectedServiceImpl}. Map is stored to user profile storage by key
 * name {@link ConnectedServiceImpl.ENTRY_NAME}
 */
public class ConnectedServices {

    /** Entry name in user profile storage. */
    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServices";

    /** Map of connected services record per relying party. */
    private Map<String, ConnectedServiceImpl> connectedServices = new HashMap<String, ConnectedServiceImpl>();

    /**
     * Constructor.
     */
    public ConnectedServices() {
    }

    /**
     * Get map of connected services record per relying party.
     * 
     * @return Map of connected services record per relying party.
     */
    public Map<String, ConnectedServiceImpl> getConnectedServices() {
        return connectedServices;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param connectedServices Json representation.
     * @return ConnectedServices parsed from json representation.
     * @throws JsonMappingException    Json contained illegal fields.
     * @throws JsonProcessingException Json is not json at all.
     */
    public static ConnectedServices parse(String connectedServices)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, ConnectedServiceImpl>> typeRef = new TypeReference<HashMap<String, ConnectedServiceImpl>>() {
        };
        Map<String, ConnectedServiceImpl> connectedOrganizations = objectMapper.readValue(connectedServices, typeRef);
        ConnectedServices services = new ConnectedServices();
        services.getConnectedServices().putAll(connectedOrganizations);
        return services;
    }

    /**
     * Serialize instance to json string.
     * 
     * @return json string representing the instance.
     * @throws JsonProcessingException something went wrong.
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getConnectedServices());
    }
}
