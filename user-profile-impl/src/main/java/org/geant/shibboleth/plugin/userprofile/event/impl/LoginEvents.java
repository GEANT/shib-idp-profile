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

package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class providing serialization and deserialization for list of
 * {@link LoginEventImpl}. List is stored to user profile storage by key name
 * {@link LoginEvents.ENTRY_NAME}
 */
public class LoginEvents {

    /** Entry name in user profile storage. */
    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents";

    /** Max number of items serialized. */
    private long maxEntries = 50;

    /** List of login events. */
    private List<LoginEventImpl> loginEvents = new ArrayList<>();

    /**
     * Constuctor.
     */
    public LoginEvents() {

    }

    /**
     * Set max number of items serialized.
     * 
     * @param maxEntries max number of items serialized.
     */
    public void setMaxEntries(long maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Get list of login events.
     * 
     * @return list of login events.
     */
    public List<LoginEventImpl> getLoginEvents() {
        return loginEvents;
    }

    /**
     * Parse instance from json representation.
     * 
     * @param loginEvents json representation.
     * @return LoginEvents parsed from json representation.
     * @throws JsonMappingException    json contained illegal fields
     * @throws JsonProcessingException json is not json at all
     */
    public static LoginEvents parse(String loginEvents) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        LoginEventImpl[] accessTokens = objectMapper.readValue(loginEvents, LoginEventImpl[].class);
        LoginEvents events = new LoginEvents();
        events.loginEvents = new ArrayList<LoginEventImpl>(Arrays.asList(accessTokens));
        return events;
    }

    /**
     * Serialize instance to json string. Serialized instance has a maximum number
     * of entries that defaults to 50.
     * 
     * @return json string representing the instance.
     * @throws JsonProcessingException something went wrong.
     */
    public String serializeWithMaxEntries() throws JsonProcessingException {
        while (getLoginEvents().size() > maxEntries) {
            getLoginEvents().remove(0);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getLoginEvents());

    }
}
