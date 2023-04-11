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
                " {\"rpId\":\"foo\",\"times\":2,\"lastAttributes\":[{\"id\":\"id\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]},{\"id\":\"id2\",\"name\":\"name\",\"description\":\"desc\",\"values\":[\"foo\",\"bar\"]}]} ");
        connectedOrganization = ConnectedOrganizationImpl.parse(connectedOrganization.serialize());
        Assert.assertEquals(connectedOrganization.getRpId(), "foo");
        Assert.assertEquals(connectedOrganization.getTimes(), 2);
        Assert.assertEquals(connectedOrganization.getLastAttributes().size(), 2);
    }
}
