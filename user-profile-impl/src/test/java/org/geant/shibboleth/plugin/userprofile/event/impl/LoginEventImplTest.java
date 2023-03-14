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
        LoginEventImpl connectedOrganization = LoginEventImpl
                .parse(" {\"rpId\":\"foo\",\"time\":1010102,\"attributes\":[\"foo\",\"bar\"]} ");
        Assert.assertEquals(connectedOrganization.getRpId(), "foo");
        Assert.assertEquals(connectedOrganization.getTime(), 1010102);
        Assert.assertTrue(connectedOrganization.getAttributes().contains("foo"));
        Assert.assertTrue(connectedOrganization.getAttributes().contains("bar"));

    }
}
