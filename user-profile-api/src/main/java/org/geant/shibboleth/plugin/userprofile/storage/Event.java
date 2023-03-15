package org.geant.shibboleth.plugin.userprofile.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Event {

    /** Event value. */
    private final String value;
    /** Event time. */
    private final long time;

    /**
     * Get event value.
     * 
     * @return Event value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get event time.
     * 
     * @return Event time
     */
    public long getTime() {
        return time;
    }

    /** Constructor. */
    Event(String value) {
        this.value = value;
        time = System.currentTimeMillis() / 1000;
    }

    /** Constructor. */
    @JsonCreator
    private Event(@JsonProperty("value") String value, @JsonProperty("time") long time) {
        this.value = value;
        this.time = time;
    }

    static Event parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, Event.class);
    }

    String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
