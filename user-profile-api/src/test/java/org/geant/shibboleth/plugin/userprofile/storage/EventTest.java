package org.geant.shibboleth.plugin.userprofile.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link Event}.
 */
public class EventTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        Event event = Event.parse("{\"value\":\"foo\",\"time\":100}");
        event = Event.parse(event.serialize());
        Assert.assertEquals(event.getTime(), 100);
        Assert.assertEquals(event.getValue(), "foo");
    }
}
