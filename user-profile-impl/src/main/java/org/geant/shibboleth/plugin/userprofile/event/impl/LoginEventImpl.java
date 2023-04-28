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

public class LoginEventImpl implements LoginEvent {

    private final String rpId;
    private final long time;
    private List<AttributeImpl> attributes;

    public LoginEventImpl(String rpId, long time, List<AttributeImpl> attributes) {
        this.rpId = rpId;
        this.time = time;
        this.attributes = attributes;
    }

    @JsonCreator
    private LoginEventImpl(@JsonProperty("rpId") String rpId,
            @JsonProperty("attributes") List<AttributeImpl> attributes, @JsonProperty("time") long time) {
        this.rpId = rpId;
        this.time = time;
        this.attributes = attributes;
    }

    public String getRpId() {
        return rpId;
    }

    public long getTime() {
        return time;
    }

    public List<? extends Attribute> getAttributes() {
        return attributes;
    }

    public static LoginEventImpl parse(String token) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(token, LoginEventImpl.class);
    }

    public String serialize() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

}
