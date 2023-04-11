package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link LoginEventImpl}.
 */
public class LoginEventImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        LoginEventImpl event = LoginEventImpl.parse(
                "{\"rpId\":\"id\",\"attributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}],\"time\":500}");
        event = LoginEventImpl.parse(event.serialize());
        Assert.assertEquals(event.getRpId(), "id");
        Assert.assertEquals(event.getTime(), 500);
        Assert.assertTrue(event.getAttributes().size() == 2);
        Assert.assertEquals(event.getAttributes().get(0).getName(), "name");
    }

}
