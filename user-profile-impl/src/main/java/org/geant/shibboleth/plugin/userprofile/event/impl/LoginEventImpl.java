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

import org.geant.shibboleth.plugin.userprofile.event.api.Attribute;
import org.geant.shibboleth.plugin.userprofile.event.api.LoginEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class implementing {@link LoginEvent} and providing serialization and
 * deserialization.
 */
public class LoginEventImpl implements LoginEvent {

    /** Relying party id of the connected service. */
    private final String id;

    /** Name of the connected service. */
    private final String name;

    /** Authentication time as seconds from epoch. */
    private final long time;

    /** Attributes sent. */
    private List<AttributeImpl> attributes;

    /** Name of the authentication context class principal. */
    private final String acr;

    /**
     * Constructor
     * 
     * @param id         relying party id of the connected service
     * @param name       name of the connected service
     * @param time       authentication time as seconds from epoch
     * @param attributes attributes sent
     */
    public LoginEventImpl(String id, String name, long time, List<AttributeImpl> attributes) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.attributes = attributes;
        acr = null;
    }

    /**
     * Constructor
     * 
     * @param id         relying party id of the connected service
     * @param name       name of the connected service
     * @param time       authentication time as seconds from epoch
     * @param attributes attributes sent
     * @param acr        name of the authentication context class principal.
     */
    public LoginEventImpl(String id, String name, long time, List<AttributeImpl> attributes, String acr) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.attributes = attributes;
        this.acr = acr;
    }

    /**
     * Constructor for json representation.
     * 
     * @param id         relying party id of the connected service
     * @param name       name of the connected service
     * @param attributes attributes sent
     * @param time       authentication time as seconds from epoch
     */
    @JsonCreator
    private LoginEventImpl(@JsonProperty("id") String id, @JsonProperty("name") String name,
            @JsonProperty("attributes") List<AttributeImpl> attributes, @JsonProperty("time") long time) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.attributes = attributes;
        this.acr = null;
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
    public long getTime() {
        return time;
    }

    /** {@inheritDoc} */
    public String getAcr() {
        return acr;
    }

    /** {@inheritDoc} */
    public List<? extends Attribute> getAttributes() {
        return attributes;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param loginEvent json representation
     * @return LoginEventImpl parsed from json representation.
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json is not json at all
     */
    public static LoginEventImpl parse(String loginEvent) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(loginEvent, LoginEventImpl.class);
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
