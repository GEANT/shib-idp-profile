package org.geant.shibboleth.plugin.userprofile.context;

import org.testng.annotations.Test;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import net.minidev.json.JSONObject;
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
        Assert.assertNotNull(ctx.getRPRelyingPartyUIContextes());
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
    public void testRelyingParties() {
        Assert.assertNotNull(ctx.getRelyingParties());
        ctx.addRelyingParty("id-1", "first", "oidc");
        ctx.addRelyingParty("id-2", "second", "saml2");
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
        ctx.setEncodedJSONAttribute("id-1", new IdPAttribute("attrId-1"), new JSONObject());
        ctx.setEncodedJSONAttribute("id-2", new IdPAttribute("attrId-2"), new JSONObject());
        Assert.assertEquals(ctx.getRPEncodedJSONAttributes().size(), 2);
    }

}