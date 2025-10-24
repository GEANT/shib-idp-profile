/*
 * Copyright (c) 2022-2025, GÉANT
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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Events stored to user profile storage.
 */
public class Events {

    /** Map of events, instances by name. */
    @Nonnull
    private Map<String, Event> events = new HashMap<>();

    /**
     * Constructor.
     */
    Events() {
    }

    /**
     * Get map of events, instances by name.
     *
     * @return map of events, instances by name
     */
    @Nonnull
    public Map<String, Event> getEvents() {
        return events;
    }

    /**
     * Parse instance from json string representation.
     * 
     * @param tokens json string representation
     * @return event instance parsed from json string representation
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json string is most likely malformatted
     */
    public static Events parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, Event>> typeRef = new TypeReference<HashMap<String, Event>>() {
        };
        Map<String, Event> events = objectMapper.readValue(tokens, typeRef);
        Events eventsObject = new Events();
        eventsObject.getEvents().putAll(events);
        return eventsObject;
    }

    /**
     * Serialize the instance to json string.
     * 
     * @return json string representation
     * @throws JsonProcessingException mapping instance values to json failed
     */
    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getEvents());
    }

}
