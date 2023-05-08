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
import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Attribute;
import org.geant.shibboleth.plugin.userprofile.event.api.ConnectedService;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class implementing {@link ConnectedService} and providing serialization and
 * deserialization.
 */
public class ConnectedServiceImpl implements ConnectedService {

    /** Relying party id of the connected service. */
    private final String id;

    /** Name of the connected service. */
    private final String name;

    /** Number of times authenticated to connected service. */
    private long times;

    /** Attributes sent in last authentication. */
    private final List<AttributeImpl> lastAttributes;

    /**
     * Constructor.
     * 
     * @param id   Relying party id of the connected service.
     * @param name Name of the connected service.
     */
    public ConnectedServiceImpl(String id, String name) {
        this.id = id;
        this.name = name;
        times = 0;
        lastAttributes = new ArrayList<AttributeImpl>();
    }

    /**
     * Constructor for json representation.
     * 
     * @param id             Relying party id of the connected service.
     * @param name           Name of the connected service.
     * @param lastAttributes Attributes sent in last authentication.
     * @param times          Number of times authenticated to connected service.
     */
    @JsonCreator
    private ConnectedServiceImpl(@JsonProperty("id") String id, @JsonProperty("name") String name,
            @JsonProperty("lastAttributes") List<AttributeImpl> lastAttributes, @JsonProperty("times") long times) {
        this.id = id;
        this.name = name;
        this.lastAttributes = lastAttributes;
        this.times = times;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public long getTimes() {
        return times;
    }

    /** {@inheritDoc} */
    public List<? extends Attribute> getLastAttributes() {
        return lastAttributes;
    }

    /**
     * Get attributes sent in last authentication.
     * 
     * @return attributes sent in last authentication.
     */
    @JsonIgnore
    public List<AttributeImpl> getLastAttributesImpl() {
        return lastAttributes;
    }

    /**
     * Add +1 to times authenticated to connected service.
     * 
     * @return
     */
    public long addCount() {
        return ++times;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param connectedService Json representation
     * @return ConnectedServiceImpl parsed from json representation
     * @throws JsonMappingException    Json contained illegal fields
     * @throws JsonProcessingException Json is not json at all
     */
    public static ConnectedServiceImpl parse(String connectedService)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(connectedService, ConnectedServiceImpl.class);
    }

    /**
     * Serialize instance to json string.
     * 
     * @return json string representing the instance
     * @throws JsonProcessingException something went wrong
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
