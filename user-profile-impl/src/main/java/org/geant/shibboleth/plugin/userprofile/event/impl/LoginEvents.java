package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginEvents {

    public final static String ENTRY_NAME = "org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents";

    private long maxEntries = 10;

    private List<LoginEventImpl> loginEvents = new ArrayList<LoginEventImpl>();

    public void setMaxEntries(long maxEntries) {
        this.maxEntries = maxEntries;
    }

    public List<LoginEventImpl> getLoginEvents() {
        return loginEvents;
    }

    public LoginEvents() {

    }

    public static LoginEvents parse(String tokens) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        LoginEventImpl[] accessTokens = objectMapper.readValue(tokens, LoginEventImpl[].class);
        LoginEvents accTokens = new LoginEvents();
        accTokens.loginEvents = new ArrayList<LoginEventImpl>(Arrays.asList(accessTokens));
        return accTokens;
    }

    public String serialize() throws JsonProcessingException {
        while (getLoginEvents().size() > maxEntries) {
            getLoginEvents().remove(0);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(getLoginEvents());

    }

}
