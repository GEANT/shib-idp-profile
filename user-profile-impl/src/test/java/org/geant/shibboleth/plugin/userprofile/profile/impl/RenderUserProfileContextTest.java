package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.impl.SAMLMetadataLookupHandlerTest;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.JsonObject;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.oidc.metadata.impl.FilesystemClientInformationResolver;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
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
        metadataResolver.setParserPool(parserPool);
        metadataResolver.setId("md");
        metadataResolver.initialize();
        action = new RenderUserProfileContext();
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(new JsonObject()), true);
        // TODO: Fix SAML2 test.
        // userProfileContext.setEntityDescriptors(metadataResolver.resolve(new
        // CriteriaSet(new SatisfyAnyCriterion(),
        // new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME))));
        userProfileContext.setOidcClientInformation(resolver.resolve(new CriteriaSet()));

    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(userProfileContext.getRelyingParties().keySet().size(), 9);
        Assert.assertEquals(userProfileContext.getRPRelyingPartyUIContextes().keySet().size(), 9);
    }

}
