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
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(), true);
        userProfileContext.getRelyingParties().put("rpId-1", null);
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
        userProfileContext.getRelyingParties().put("rpId-2", null);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        Assert.assertEquals(rpCtx.getRelyingPartyId(), "rpId-2");
    }
}
