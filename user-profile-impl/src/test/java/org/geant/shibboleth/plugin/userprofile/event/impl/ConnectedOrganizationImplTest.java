package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link ConnectedOrganizationImpl}.
 */
public class ConnectedOrganizationImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        ConnectedOrganizationImpl connectedOrganization = ConnectedOrganizationImpl.parse(
                " {\"rpId\":\"foo\",\"times\":2,\"lastAttributes\":[\"foo\",\"bar\"]} ");
        Assert.assertEquals(connectedOrganization.getRpId(), "foo");
        Assert.assertEquals(connectedOrganization.getTimes(), 2);
        Assert.assertTrue(connectedOrganization.getLastAttributes().contains("foo"));
        Assert.assertTrue(connectedOrganization.getLastAttributes().contains("bar"));
        
    }
}
