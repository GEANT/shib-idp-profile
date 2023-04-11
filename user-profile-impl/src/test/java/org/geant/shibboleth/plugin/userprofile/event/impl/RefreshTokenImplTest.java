package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link RefreshTokenImpl}.
 */
public class RefreshTokenImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        RefreshTokenImpl refreshToken = RefreshTokenImpl.parse(
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\", \"scope\":[\"openid\",\"profile\"],\"exp\":100} ");
        refreshToken = RefreshTokenImpl.parse(refreshToken.serialize());
        Assert.assertEquals(refreshToken.getTokenId(), "_0101");
        Assert.assertEquals(refreshToken.getTokenRootId(), "_0102");
        Assert.assertEquals(refreshToken.getClientId(), "foo");
        Assert.assertTrue(refreshToken.getScope().contains("openid"));
        Assert.assertTrue(refreshToken.getScope().contains("profile"));
        Assert.assertEquals(refreshToken.getExp(), 100);
    }
}
