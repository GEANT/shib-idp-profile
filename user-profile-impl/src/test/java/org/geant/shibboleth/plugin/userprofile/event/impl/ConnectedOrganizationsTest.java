package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link ConnectedOrganizations}.
 */
public class ConnectedOrganizationsTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        ConnectedOrganizations connectedOrganizations = ConnectedOrganizations.parse(
                " { \"foo\" : {\"rpId\":\"foo\",\"times\":2,\"lastAttributes\":[\"foo\",\"bar\"]}, "
                + "  \"bar\" : {\"rpId\":\"bar\",\"times\":3,\"lastAttributes\":[\"foo\",\"bar\"]}} ");
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().size(), 2);
        connectedOrganizations =  ConnectedOrganizations.parse(connectedOrganizations.serialize());
        Assert.assertTrue(connectedOrganizations.getConnectedOrganization().keySet().contains("foo"));
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("foo").getRpId(), "foo");
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("foo").getTimes(), 2);
        Assert.assertTrue(connectedOrganizations.getConnectedOrganization().get("foo").getLastAttributes().contains("foo"));
        Assert.assertTrue(connectedOrganizations.getConnectedOrganization().keySet().contains("bar"));
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("bar").getRpId(), "bar");
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().size(), 2);
        
        
    }
}
