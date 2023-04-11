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
                " { \"foo\" : {\"rpId\":\"foo\",\"times\":2,\"lastAttributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id2\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}]}, "
                        + "  \"bar\" : {\"rpId\":\"bar\",\"times\":3,\"lastAttributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id2\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}]}} ");
        connectedOrganizations = ConnectedOrganizations.parse(connectedOrganizations.serialize());
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().size(), 2);
        Assert.assertTrue(connectedOrganizations.getConnectedOrganization().keySet().contains("foo"));
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("foo").getRpId(), "foo");
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("foo").getTimes(), 2);
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("foo").getLastAttributes().size(), 2);
        Assert.assertTrue(connectedOrganizations.getConnectedOrganization().keySet().contains("bar"));
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().get("bar").getRpId(), "bar");
        Assert.assertEquals(connectedOrganizations.getConnectedOrganization().size(), 2);

    }
}
