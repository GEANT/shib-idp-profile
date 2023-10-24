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

import java.time.Duration;
import java.time.Instant;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokenImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.impl.MemoryStorageService;
import org.opensaml.storage.impl.StorageServiceRevocationCache;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.plugin.oidc.op.storage.RevocationCacheContexts;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.NonnullSupplier;

/**
 * Unit tests for {@link ExtractRelyingPartyIdFromRequest}.
 */
public class ExtractAndRevokeTokenFromRequestTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private UserProfileContext userProfileContext;

    private ExtractAndRevokeTokenFromRequest action;

    private MemoryStorageService storageService;
    private StorageServiceRevocationCache revocationCache;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException, JsonMappingException, JsonProcessingException {
        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        revocationCache = new StorageServiceRevocationCache();
        revocationCache.setId("test-2");
        revocationCache.setEntryExpiration(Duration.ofHours(1));
        revocationCache.setStorage(storageService);
        revocationCache.initialize();

        action = new ExtractAndRevokeTokenFromRequest();
        action.setRevocationCache(revocationCache);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("_eventId_revokeToken", "_1234");
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(), true);
        Long exp = Instant.now().plus(Duration.ofSeconds(10)).getEpochSecond();
        userProfileContext.addAccessToken("rp1", AccessTokenImpl.parse(
                " {\"tokenId\":\"_2345\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + exp.toString() + "} "));
        userProfileContext.addAccessToken("rp1", AccessTokenImpl.parse(
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + exp.toString() + "} "));
        userProfileContext.addAccessToken("rp2", AccessTokenImpl.parse(
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + exp.toString() + "} "));
        userProfileContext.addAccessToken("rp2", AccessTokenImpl.parse(
                " {\"tokenId\":\"_1234\",\"tokenRootId\":\"_0102\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + exp.toString() + "} "));
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(revocationCache.isRevoked(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, "_1234"));
    }

    @AfterMethod
    protected void tearDown() {
        action.destroy();
        revocationCache.destroy();
        storageService.destroy();
    }

    @Test
    public void testNoServlet() throws ComponentInitializationException {
        action.setHttpServletRequestSupplier(null);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    @Test
    public void testNoParameter() throws ComponentInitializationException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
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
        action.setTokenIdFieldName("_eventId_revokeToken2");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("_eventId_revokeToken2", "_2345");
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(revocationCache.isRevoked(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, "_2345"));
    }
}
