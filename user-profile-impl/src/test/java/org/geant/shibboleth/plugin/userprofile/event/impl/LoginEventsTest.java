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
        LoginEvents loginEvents = LoginEvents.parse(
                " [{\"rpId\":\"id\",\"attributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}],\"time\":500}, "
                        + "  {\"rpId\":\"id\",\"attributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}],\"time\":500}] ");
        loginEvents = LoginEvents.parse(loginEvents.serialize());
        Assert.assertEquals(loginEvents.getLoginEvents().size(), 2);
        AttributeImpl attribute = new AttributeImpl("id", "name", "descr", Arrays.asList("foo"));
        loginEvents.getLoginEvents()
                .add(new LoginEventImpl("rpIdNew1", 2010104, Arrays.asList(attribute, attribute, attribute)));
        loginEvents.getLoginEvents()
                .add(new LoginEventImpl("rpIdNew2", 2010104, Arrays.asList(attribute, attribute, attribute)));
        loginEvents.getLoginEvents()
                .add(new LoginEventImpl("rpIdNew3", 2010104, Arrays.asList(attribute, attribute, attribute)));
        loginEvents.getLoginEvents()
                .add(new LoginEventImpl("rpIdNew4", 2010104, Arrays.asList(attribute, attribute, attribute)));
        loginEvents.getLoginEvents()
                .add(new LoginEventImpl("rpIdNew5", 2010104, Arrays.asList(attribute, attribute, attribute)));
        loginEvents.setMaxEntries(5);
        loginEvents = LoginEvents.parse(loginEvents.serialize());
        Assert.assertEquals(loginEvents.getLoginEvents().size(), 5);
        Assert.assertTrue(loginEvents.getLoginEvents().get(0).getRpId().equals("rpIdNew1"));
    }

}
