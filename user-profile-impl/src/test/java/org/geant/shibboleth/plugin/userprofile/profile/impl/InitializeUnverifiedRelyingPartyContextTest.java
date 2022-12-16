package org.geant.shibboleth.plugin.userprofile.profile.impl;

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
 * Unit tests for {@link InitializeUnverifiedRelyingPartyContext}.
 */
public class InitializeUnverifiedRelyingPartyContextTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private InitializeUnverifiedRelyingPartyContext action;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException {
        action = new InitializeUnverifiedRelyingPartyContext();
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(prc.getSubcontext(RelyingPartyContext.class));
    }
 
}
