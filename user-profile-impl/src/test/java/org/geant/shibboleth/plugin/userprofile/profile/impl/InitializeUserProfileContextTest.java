package org.geant.shibboleth.plugin.userprofile.profile.impl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.impl.SAMLMetadataLookupHandlerTest;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.oidc.metadata.impl.FilesystemClientInformationResolver;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Unit tests for {@link InitializeUserProfileContext}.
 */
public class InitializeUserProfileContextTest extends XMLObjectBaseTestCase {

    private RequestContext src;

    private ProfileRequestContext prc;

    private FilesystemMetadataResolver metadataResolver;

    private FilesystemClientInformationResolver resolver;

    private InitializeUserProfileContext action;

    private UserProfileCache userProfileCache;

    private MemoryStorageService storageService;

    @BeforeClass
    public void classSetUp() throws ResolverException, URISyntaxException, ComponentInitializationException {

    }

    @AfterMethod
    protected void tearDown() {
        userProfileCache.destroy();
        userProfileCache = null;

        storageService.destroy();
        storageService = null;
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

        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        userProfileCache = new UserProfileCache();
        userProfileCache.setRecordExpiration(Duration.ofMillis(500));
        userProfileCache.setStorage(storageService);
        userProfileCache.initialize();

        action = new InitializeUserProfileContext();
        action.setUserProfileCache(userProfileCache);
        action.setMetadataResolver(metadataResolver);
        action.setClientInformationResolver(resolver);

        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        SubjectContext subjContext = new SubjectContext();
        subjContext.setPrincipalName("userprincipal");
        prc.addSubcontext(subjContext);

    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(prc.getSubcontext(UserProfileContext.class));
        // TODO: fix test for SAML2 entities
        // Assert.assertEquals(((Collection<?>)(prc.getSubcontext(UserProfileContext.class).getEntityDescriptors())).size(),
        // 5);
        Assert.assertEquals(
                ((Collection<?>) (prc.getSubcontext(UserProfileContext.class).getOidcClientInformation())).size(), 9);
    }

}
