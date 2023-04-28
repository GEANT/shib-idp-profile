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

public class ConnectedOrganizations {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedOrganizations";

    private Map<String, ConnectedOrganizationImpl> connectedOrganizations = new HashMap<String, ConnectedOrganizationImpl>();

    public Map<String, ConnectedOrganizationImpl> getConnectedOrganization() {
        return connectedOrganizations;
    }

    public ConnectedOrganizations() {
    }

    public static ConnectedOrganizations parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, ConnectedOrganizationImpl>> typeRef = new TypeReference<HashMap<String, ConnectedOrganizationImpl>>() {
        };
        Map<String, ConnectedOrganizationImpl> connectedOrganizations = objectMapper.readValue(tokens, typeRef);
        ConnectedOrganizations connectedOrganizationsObject = new ConnectedOrganizations();
        connectedOrganizationsObject.getConnectedOrganization().putAll(connectedOrganizations);
        return connectedOrganizationsObject;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getConnectedOrganization());
    }
}
