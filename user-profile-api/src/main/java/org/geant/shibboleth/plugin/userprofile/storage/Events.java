package org.geant.shibboleth.plugin.userprofile.storage;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Events {

    private Map<String, Event> events = new HashMap<String, Event>();

    public Map<String, Event> getEvents() {
        return events;
    }

    Events() {
    }

    public static Events parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, Event>> typeRef = new TypeReference<HashMap<String, Event>>() {
        };
        Map<String, Event> events = objectMapper.readValue(tokens, typeRef);
        Events eventsObject = new Events();
        eventsObject.getEvents().putAll(events);
        return eventsObject;
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getEvents());
    }

}
