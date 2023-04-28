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
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import org.testng.annotations.BeforeMethod;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    public void testRPRelyingPartyUIContextes() {
        Assert.assertNotNull(ctx.getRelyingParties());
    }

    @Test
    public void testIdPUserAttributes() {
        Assert.assertNotNull(ctx.getIdPUserAttributes());
    }

    @Test
    public void testOidcClientInformation() {
        Assert.assertNull(ctx.getOidcClientInformation());
        List<OIDCClientInformation> rps = new ArrayList<OIDCClientInformation>();
        rps.add(new OIDCClientInformation(new ClientID(), new Date(), new OIDCClientMetadata(), null));
        rps.add(new OIDCClientInformation(new ClientID(), new Date(), new OIDCClientMetadata(), null));
        ctx.setOidcClientInformation(rps);
        Assert.assertEquals(2, ((List<?>) ctx.getOidcClientInformation()).size());
    }

    @Test
    public void testRPAttributeContext() {
        Assert.assertNotNull(ctx.getRPAttributeContext());
        ctx.setAttributeContext("id-1", new AttributeContext());
        ctx.setAttributeContext("id-2", new AttributeContext());
        Assert.assertEquals(ctx.getRPAttributeContext().size(), 2);
    }

    @Test
    public void testEncodedJSONAttribute() {
        Assert.assertNotNull(ctx.getRPEncodedJSONAttributes());
        ctx.setEncodedJSONAttribute("id-1", new IdPAttribute("attrId-1"));
        ctx.setEncodedJSONAttribute("id-2", new IdPAttribute("attrId-2"));
        Assert.assertEquals(ctx.getRPEncodedJSONAttributes().size(), 2);
    }

}