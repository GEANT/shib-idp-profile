package org.geant.shibboleth.plugin.userprofile.profile.impl;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Unit tests for {@link InitializeAuthenticationContext}.
 */
public class InitializeAuthenticationContextTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private InitializeAuthenticationContext action;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException {
        action = new InitializeAuthenticationContext();
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.setForceAuthnPredicate(Predicates.alwaysTrue());
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(prc.getSubcontext(AuthenticationContext.class));
        Assert.assertTrue(prc.getSubcontext(AuthenticationContext.class).isForceAuthn());
        
    }
 
}
