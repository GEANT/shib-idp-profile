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

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.IdPEventIds;
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

    @Test
    public void testFailedRelyingPartyContextCreation() throws ComponentInitializationException {
        action.setRelyingPartyContextCreationStrategy(new mockLookUp<>());
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    public class mockLookUp<ParentContext extends BaseContext, ChildContext extends BaseContext>
            implements ContextDataLookupFunction<ParentContext, ChildContext> {

        @Override
        public ChildContext apply(ParentContext t) {
            return null;
        }

    }

}
