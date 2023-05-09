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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.impl.SAMLMetadataLookupHandlerTest;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.index.MetadataIndex;
import org.opensaml.saml.metadata.resolver.index.impl.RoleMetadataIndex;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.oidc.metadata.impl.FilesystemClientInformationResolver;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Unit tests for {@link RenderUserProfileContext}.
 */
public class RenderUserProfileContextTest extends XMLObjectBaseTestCase {

    private RequestContext src;

    private ProfileRequestContext prc;

    private FilesystemMetadataResolver metadataResolver;

    private FilesystemClientInformationResolver resolver;

    private RenderUserProfileContext action;

    private UserProfileContext userProfileContext;

    @BeforeClass
    public void classSetUp() throws ResolverException, URISyntaxException, ComponentInitializationException {

    }

    @BeforeMethod
    public void initTests()
            throws ComponentInitializationException, URISyntaxException, ResolverException, IOException {
        final Resource file = new ClassPathResource(
                "/org/geant/shibboleth/plugin/userprofile/profile/impl/oidc-client.json");
        resolver = new FilesystemClientInformationResolver(file);
        resolver.setId("mockId");
        resolver.setFailFastInitialization(true);
        resolver.initialize();
        final URL mdURL = SAMLMetadataLookupHandlerTest.class
                .getResource("/org/geant/shibboleth/plugin/userprofile/profile/impl/metadata.xml");
        final File mdFile = new File(mdURL.toURI());
        metadataResolver = new FilesystemMetadataResolver(mdFile);
        metadataResolver.setIndexes(Collections.<MetadataIndex>singleton(new RoleMetadataIndex()));
        metadataResolver.setParserPool(parserPool);
        metadataResolver.setId("md");
        metadataResolver.initialize();
        action = new RenderUserProfileContext();
        action.setMetadataResolver(metadataResolver);
        action.setClientInformationResolver(resolver);
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(), true);
     }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(userProfileContext.getRelyingParties().keySet().size(), 9);
    }

}
