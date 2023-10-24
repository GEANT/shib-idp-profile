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

package org.geant.shibboleth.plugin.userprofile.context;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.context.AttributeContext;
import org.testng.annotations.BeforeMethod;

import java.util.List;

import org.geant.shibboleth.plugin.userprofile.event.api.AccessToken;
import org.geant.shibboleth.plugin.userprofile.event.api.Token;
import org.testng.Assert;

/**
 * Tests for {@link UserProfileCache}
 */
public class UserProfileContextTest {

    private UserProfileContext ctx = new UserProfileContext();

    @BeforeMethod
    protected void setUp() throws Exception {

    }

    @Test
    public void testInitialState() {
        Assert.assertNotNull(ctx.getIdPUserAttributes());
        Assert.assertNotNull(ctx.getConnectedOrganizations());
        Assert.assertNotNull(ctx.getAccessTokens());
        Assert.assertNotNull(ctx.getRefreshTokens());
        Assert.assertNotNull(ctx.getLoginEvents());
        Assert.assertNotNull(ctx.getRPAttributeContext());
        Assert.assertNotNull(ctx.getRelyingParties());
        Assert.assertNull(ctx.getEvents());
    }

    @Test
    public void testRPAttributeContext() {
        ctx.setAttributeContext("id-1", new AttributeContext());
        ctx.setAttributeContext("id-2", new AttributeContext());
        Assert.assertEquals(ctx.getRPAttributeContext().size(), 2);
    }

    @Test
    public void testTokenAdd() {
        ctx.addAccessToken("rpId", new mockAccessToken());
        Assert.assertEquals(ctx.getAccessTokens().size(), 1);
        ctx.addRefreshToken("rpId", new mockRefreshToken());
        Assert.assertEquals(ctx.getRefreshTokens().size(), 1);
    }

    public class mockAccessToken implements AccessToken {

        @Override
        public String getTokenId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getTokenRootId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getClientId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getScope() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getExp() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public List<String> getAudience() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public class mockRefreshToken implements Token {

        @Override
        public String getTokenId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getTokenRootId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getClientId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getScope() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getExp() {
            // TODO Auto-generated method stub
            return 0;
        }

    }

}