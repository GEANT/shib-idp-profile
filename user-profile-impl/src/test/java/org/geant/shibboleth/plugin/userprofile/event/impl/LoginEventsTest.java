package org.geant.shibboleth.plugin.userprofile.event.impl;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link LoginEvents}.
 */
public class LoginEventsTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        LoginEvents loginEvents = LoginEvents
                .parse(" [{\"rpId\":\"foo\",\"time\":1010102,\"attributes\":[\"foo\",\"bar\"]}, "
                        + "  {\"rpId\":\"foo\",\"time\":1010104,\"attributes\":[\"foo\",\"bar\"]}] ");
        Assert.assertEquals(loginEvents.getLoginEvents().size(), 2);
        loginEvents.getLoginEvents().add(new LoginEventImpl("rpIdNew1", 2010104, Arrays.asList("foo", "bar", "bar")));
        loginEvents.getLoginEvents().add(new LoginEventImpl("rpIdNew2", 2010104, Arrays.asList("foo", "bar", "bar")));
        loginEvents.getLoginEvents().add(new LoginEventImpl("rpIdNew3", 2010104, Arrays.asList("foo", "bar", "bar")));
        loginEvents.getLoginEvents().add(new LoginEventImpl("rpIdNew4", 2010104, Arrays.asList("foo", "bar", "bar")));
        loginEvents.getLoginEvents().add(new LoginEventImpl("rpIdNew5", 2010104, Arrays.asList("foo", "bar", "bar")));
        loginEvents.setMaxEntries(5);
        loginEvents = LoginEvents.parse(loginEvents.serialize());
        Assert.assertEquals(loginEvents.getLoginEvents().size(), 5);
        Assert.assertTrue(loginEvents.getLoginEvents().get(0).getRpId().equals("rpIdNew1"));
    }
}
