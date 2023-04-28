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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.attribute.transcoding.impl.AttributeTranscoderRegistryImpl;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.oidc.attribute.transcoding.AbstractOIDCAttributeTranscoder;
import net.shibboleth.oidc.attribute.transcoding.OIDCAttributeTranscoder;
import net.shibboleth.oidc.attribute.transcoding.impl.OIDCStringAttributeTranscoder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.test.service.MockReloadableService;

/**
 * Unit tests for {@link RenderRPAttributes}.
 */
public class RenderRPAttributesTest {

    private AttributeTranscoderRegistryImpl registry;

    private RequestContext src;

    private ProfileRequestContext prc;

    private UserProfileContext userProfileContext;

    private RelyingPartyContext relyingPartyContext;

    private RenderRPAttributes action;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException {
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");

        final OIDCStringAttributeTranscoder transcoder = new OIDCStringAttributeTranscoder();
        transcoder.initialize();

        final Map<String, Object> rule1 = new HashMap<>();
        rule1.put(AttributeTranscoderRegistry.PROP_ID, "attribute_0");
        rule1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule1.put(OIDCAttributeTranscoder.PROP_NAME, "attribute_0");

        final Map<String, Object> rule2 = new HashMap<>();
        rule2.put(AttributeTranscoderRegistry.PROP_ID, "attribute_1");
        rule2.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule2.put(OIDCAttributeTranscoder.PROP_NAME, "attribute_1");

        final Map<String, Object> rule3 = new HashMap<>();
        rule3.put(AttributeTranscoderRegistry.PROP_ID, "attribute_2");
        rule3.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        rule3.put(OIDCAttributeTranscoder.PROP_NAME, "attribute_2");

        registry.setNamingRegistry(Collections.singletonList(new BasicNamingFunction<>(transcoder.getEncodedType(),
                new AbstractOIDCAttributeTranscoder.NamingFunction())));

        registry.setTranscoderRegistry(
                List.of(new TranscodingRule(rule1), new TranscodingRule(rule2), new TranscodingRule(rule3)));

        registry.setApplicationContext(new MockApplicationContext());
        registry.initialize();

        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        action = new RenderRPAttributes();
        action.setTranscoderRegistry(new MockReloadableService<>(registry));
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(), true);
        relyingPartyContext = (RelyingPartyContext) prc.addSubcontext(new RelyingPartyContext(), true);
        relyingPartyContext.setRelyingPartyId("rpId");

        AttributeContext attributeContext = new AttributeContext();
        IdPAttribute idPAttribute_0 = new IdPAttribute("attribute_0");
        idPAttribute_0.setValues(Arrays.asList(new StringAttributeValue("a")));
        IdPAttribute idPAttribute_1 = new IdPAttribute("attribute_1");
        IdPAttribute idPAttribute_2 = new IdPAttribute("attribute_2");
        idPAttribute_2.setValues(Arrays.asList(new StringAttributeValue("b")));
        IdPAttribute idPAttribute_3 = new IdPAttribute("attribute_3");
        idPAttribute_3.setValues(Arrays.asList(new StringAttributeValue("c")));
        attributeContext
                .setIdPAttributes(Arrays.asList(idPAttribute_0, idPAttribute_1, idPAttribute_2, idPAttribute_3));
        userProfileContext.setAttributeContext("rpId", attributeContext);

    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        // Two of the attributes have value and transcoder.
        Assert.assertEquals(userProfileContext.getRPEncodedJSONAttributes().get("rpId").size(), 2);
    }

}
