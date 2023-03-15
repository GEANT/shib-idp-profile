package org.geant.shibboleth.plugin.userprofile.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link Events}.
 */
public class EventsTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        Events events = Events
                .parse("{\"key1\" : {\"value\":\"foo1\",\"time\":100}, \"key2\" : {\"value\":\"foo2\",\"time\":100} }");
        events = Events.parse(events.serialize());
        Assert.assertEquals(events.getEvents().size(), 2);
        Assert.assertEquals(events.getEvents().get("key1").getValue(), "foo1");
    }
}
