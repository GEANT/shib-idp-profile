package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link AccessTokens}.
 */
public class AccessTokensTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        AccessTokens accessTokens = AccessTokens.parse(
                " [{\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":100}," +
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":100}] ");
        Assert.assertEquals(accessTokens.getAccessTokens().size(),2);
        String serialized=accessTokens.serialize();
        accessTokens = AccessTokens.parse(serialized);
        Assert.assertEquals(accessTokens.getAccessTokens().size(),2);
        
    }
}
