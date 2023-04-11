package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.Attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AttributeImpl implements Attribute {

    private final String id;
    private final String name;
    private final String description;
    private final List<String> values;

    public AttributeImpl(String id, String name, String description, List<String> values) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.values = values;
    }

    @JsonCreator
    private AttributeImpl(@JsonProperty("id") String id, @JsonProperty("name") String name,
            @JsonProperty("description") String description, @JsonProperty("values") List<String> values,
            @JsonProperty("time") long time) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getValues() {
        return values;
    }

    public static AttributeImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, AttributeImpl.class);
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

    @Override
    @JsonIgnore
    public String getDisplayValue() {
        return values != null ? name + "[" + String.join(",", values) + "]" : name;
    }

}
