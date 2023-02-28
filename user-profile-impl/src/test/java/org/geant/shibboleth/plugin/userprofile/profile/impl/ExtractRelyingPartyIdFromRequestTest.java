package org.geant.shibboleth.plugin.userprofile.profile.impl;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.minidev.json.JSONObject;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Unit tests for {@link ExtractRelyingPartyIdFromRequest}.
 */
public class ExtractRelyingPartyIdFromRequestTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private UserProfileContext userProfileContext;

    private ExtractRelyingPartyIdFromRequest action;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException {
        action = new ExtractRelyingPartyIdFromRequest();
        action.setHttpServletRequest(new MockHttpServletRequest());
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("_eventId_showAttributes", "rpId-1");
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(new JSONObject()), true);
        userProfileContext.addRelyingParty("rpId-1", "first", "oidc");
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        Assert.assertEquals(rpCtx.getRelyingPartyId(), "rpId-1");
    }

    @Test
    public void testNoServlet() throws ComponentInitializationException {
        action.setHttpServletRequest(null);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    @Test
    public void testNoParameter() throws ComponentInitializationException {
        action.setHttpServletRequest(new MockHttpServletRequest());
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    @Test
    public void testNoUserProfileContext() throws ComponentInitializationException {
        prc.removeSubcontext(userProfileContext);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    @Test
    public void testParameterNameChange() throws ComponentInitializationException {
        action.setRpIdFieldName("_eventId_showAttributes2");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("_eventId_showAttributes2", "rpId-2");
        userProfileContext.addRelyingParty("rpId-2", "second", "saml2");
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        Assert.assertEquals(rpCtx.getRelyingPartyId(), "rpId-2");
    }
}
