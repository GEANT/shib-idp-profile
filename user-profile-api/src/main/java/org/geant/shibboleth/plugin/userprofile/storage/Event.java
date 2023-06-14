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

package org.geant.shibboleth.plugin.userprofile.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Event stored to user profile storage.
 */
public class Event {

    /** Event value. */
    private final String value;

    /** Event time as seconds from epoch. */
    private final long time;

    /**
     * Constructor.
     * 
     * @param value event value
     * @param time  event time as seconds from epoch
     */
    @JsonCreator
    private Event(@JsonProperty("value") String value, @JsonProperty("time") long time) {
        this.value = value;
        this.time = time;
    }

    /**
     * Get event value.
     * 
     * @return event value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get event time as seconds from epoch.
     * 
     * @return event time
     */
    public long getTime() {
        return time;
    }

    /**
     * Constructor.
     * 
     * @param value event value
     */
    Event(String value) {
        this.value = value;
        time = System.currentTimeMillis() / 1000;
    }

    /**
     * Parse instance from json string representation.
     * 
     * @param token json string representation
     * @return event instance parsed from json string representation
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json string is most likely malformatted
     */
    static Event parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, Event.class);
    }

    /**
     * Serialize the instance to json string.
     * 
     * @return json string representation
     * @throws jsonProcessingException Mapping instance values to json failed.
     */
    String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}