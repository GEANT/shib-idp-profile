/*
 * Copyright (c) 2022-2023, GÉANT
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geant.shibboleth.plugin.userprofile.event.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Unit tests for {@link AccessTokenImpl}.
 */
public class AccessTokenImplTest {

    @Test
    public void test() throws JsonMappingException, JsonProcessingException {
        AccessTokenImpl accessToken = AccessTokenImpl.parse(
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":100} ");
        accessToken = AccessTokenImpl.parse(accessToken.serialize());
        Assert.assertEquals(accessToken.getTokenId(), "_0101");
        Assert.assertEquals(accessToken.getTokenRootId(), "_0102");
        Assert.assertEquals(accessToken.getClientId(), "foo");
        Assert.assertTrue(accessToken.getAudience().contains("foo"));
        Assert.assertTrue(accessToken.getAudience().contains("bar"));
        Assert.assertTrue(accessToken.getScope().contains("openid"));
        Assert.assertTrue(accessToken.getScope().contains("profile"));
        Assert.assertEquals(accessToken.getExp(), 100);
    }
}
