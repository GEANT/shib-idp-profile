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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class implementing {@link Attribute} and providing serialization and
 * deserialization.
 */
public class AttributeImpl implements Attribute {

    /** Attribute id. */
    private final String id;

    /** Attribute name. */
    private final String name;

    /** Attribute description. */
    private final String description;

    /** Attribute values. */
    private final List<String> values;

    /**
     * Constructor.
     * 
     * @param id          attribute id
     * @param name        attribute name
     * @param description attribute description
     * @param values      attribute values
     */
    @JsonCreator
    public AttributeImpl(@JsonProperty("id") String id, @JsonProperty("name") String name,
            @JsonProperty("description") String description, @JsonProperty("values") List<String> values) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.values = values;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getValues() {
        return values;
    }

    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public String getDisplayValue() {
        return values != null ? name + " [ " + String.join(",", values) + " ] " : name;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param attribute json string representing the instance
     * @return AttributeImpl parsed from json representation
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json is not json at all
     */
    public static AttributeImpl parse(String attribute) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(attribute, AttributeImpl.class);
    }

    /**
     * Serialize instance to json string.
     * 
     * @return json string representing the instance.
     * @throws JsonProcessingException something went wrong.
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
