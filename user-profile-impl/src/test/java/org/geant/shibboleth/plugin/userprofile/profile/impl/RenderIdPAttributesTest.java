package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.util.Arrays;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.minidev.json.JSONObject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Unit tests for {@link RenderIdPAttributes}.
 */
public class RenderIdPAttributesTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private UserProfileContext userProfileContext;

    private RenderIdPAttributes action;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException {
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        action = new RenderIdPAttributes();
        action.setIdPUserAttributes(Arrays.asList("attribute_1", "attribute_2"));
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(new JSONObject()), true);
        AttributeContext attributeContext = new AttributeContext();
        IdPAttribute idPAttribute_0 = new IdPAttribute("attribute_0");
        idPAttribute_0.setValues(Arrays.asList(new StringAttributeValue("0")));
        IdPAttribute idPAttribute_1 = new IdPAttribute("attribute_1");
        IdPAttribute idPAttribute_2 = new IdPAttribute("attribute_2");
        idPAttribute_2.setValues(Arrays.asList(new StringAttributeValue("2")));
        IdPAttribute idPAttribute_3 = new IdPAttribute("attribute_3");
        idPAttribute_3.setValues(Arrays.asList(new StringAttributeValue("3")));
        attributeContext
                .setIdPAttributes(Arrays.asList(idPAttribute_0, idPAttribute_1, idPAttribute_2, idPAttribute_3));
        userProfileContext.setAttributeContext(null, attributeContext);
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(userProfileContext.getIdPUserAttributes().size(), 1);
    }

}
