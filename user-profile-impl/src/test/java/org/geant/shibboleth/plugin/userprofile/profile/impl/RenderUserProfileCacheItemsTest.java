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
import java.util.function.Function;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileContext;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokenImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServiceImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.ConnectedServices;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEventImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.LoginEvents;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokenImpl;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.RevocationCache;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.shibboleth.idp.plugin.oidc.op.storage.RevocationCacheContexts;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Unit tests for {@link RenderUserProfileCacheItems}.
 */
public class RenderUserProfileCacheItemsTest {

    private MemoryStorageService storageService;

    private UserProfileCache userProfileCache;

    private RevocationCache revocationCache;

    private RequestContext src;

    private ProfileRequestContext prc;

    private RenderUserProfileCacheItems action;

    private UserProfileContext userProfileContext;

    @BeforeMethod
    public void initTests() throws ComponentInitializationException, JsonProcessingException {
        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        userProfileCache = new UserProfileCache();
        userProfileCache.setRecordExpiration(Duration.ofMillis(500));
        userProfileCache.setStorage(storageService);
        userProfileCache.setId("id");
        userProfileCache.initialize();
        addLoginEvents();
        addConnectedServicesEvents();
        addAccessTokenEvents();
        addRefreshTokenEvents();

        revocationCache = new RevocationCache();
        revocationCache.setStorage(storageService);
        revocationCache.setId("id");
        revocationCache.initialize();
        // Revoke one of the access tokens
        revocationCache.revoke(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, "_0104", Duration.ofSeconds(5));
        // Revoke one of the refresh tokens
        revocationCache.revoke(RevocationCacheContexts.SINGLE_ACCESS_OR_REFRESH_TOKENS, "_0204", Duration.ofSeconds(5));

        action = new RenderUserProfileCacheItems();
        action.setUserProfileCache(userProfileCache);
        action.setRevocationCache(revocationCache);
        action.setUsernameLookupStrategy(new usernameLookupStrategy());
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);
        userProfileContext = (UserProfileContext) prc.addSubcontext(new UserProfileContext(), true);
    }

    @AfterMethod
    protected void tearDown() {
        userProfileCache.destroy();
        userProfileCache = null;

        revocationCache.destroy();
        revocationCache = null;

        storageService.destroy();
        storageService = null;
    }

    @Test
    public void testSuccess() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(userProfileContext.getLoginEvents().size(), 2);
        Assert.assertEquals(userProfileContext.getConnectedOrganizations().size(), 1);
        Assert.assertEquals(userProfileContext.getAccessTokens().get("foo").size(), 2);
        Assert.assertEquals(userProfileContext.getRefreshTokens().get("foo").size(), 2);
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testFailPostInitSetUserProfileContextLookupStrategy() throws ComponentInitializationException {
        action.initialize();
        action.setUserProfileContextLookupStrategy(new ChildContextLookup<>(UserProfileContext.class));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testFailNullUserProfileContextLookupStrategy() throws ComponentInitializationException {
        action.setUserProfileContextLookupStrategy(null);
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testFailPostInitSetSubjectContextLookupStrategy() throws ComponentInitializationException {
        action.initialize();
        action.setUsernameLookupStrategy(new usernameLookupStrategy());
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testFailNullSubjectContextLookupStrategy() throws ComponentInitializationException {
        action.setUsernameLookupStrategy(null);
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testFailPostInitSetRevocationCache() throws ComponentInitializationException {
        action.initialize();
        action.setRevocationCache(revocationCache);
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testFailPostInitSetUserProfileCache() throws ComponentInitializationException {
        action.initialize();
        action.setUserProfileCache(userProfileCache);
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testFailNullUserProfileCache() throws ComponentInitializationException {
        action.setUserProfileCache(null);
    }

    public void testFailInvalidProfileContext() throws ComponentInitializationException {
        prc.removeSubcontext(UserProfileContext.class);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    public void testFailInvalidProfileContext2() throws ComponentInitializationException {
        action.setUserProfileContextLookupStrategy(new mockLookUp<ProfileRequestContext, UserProfileContext>());
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    private void addLoginEvents() throws JsonProcessingException {
        LoginEvents events = new LoginEvents();
        events.getLoginEvents().add(new LoginEventImpl("rpId", "ServiceName", System.currentTimeMillis() / 1000, null));
        events.getLoginEvents()
                .add(new LoginEventImpl("rpId2", "ServiceName2", System.currentTimeMillis() / 1000, null));
        userProfileCache.setSingleEvent(new usernameLookupStrategy().apply(prc), LoginEvents.ENTRY_NAME,
                events.serialize());
    }

    private void addConnectedServicesEvents() throws JsonProcessingException {
        ConnectedServices services = new ConnectedServices();
        services.getConnectedServices().put("rpId", new ConnectedServiceImpl("rpId", "ServiceName"));
        userProfileCache.setSingleEvent(new usernameLookupStrategy().apply(prc), ConnectedServices.ENTRY_NAME,
                services.serialize());
    }

    private void addAccessTokenEvents() throws JsonMappingException, JsonProcessingException {
        Long currentTime = System.currentTimeMillis() / 1000;
        AccessTokens accessTokens = new AccessTokens();
        // expired
        accessTokens.getAccessTokens().add(AccessTokenImpl.parse(
                " {\"tokenId\":\"_0101\",\"tokenRootId\":\"_01021\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime - 10) + "} "));
        // valid
        accessTokens.getAccessTokens().add(AccessTokenImpl.parse(
                " {\"tokenId\":\"_0102\",\"tokenRootId\":\"_01022\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        // valid
        accessTokens.getAccessTokens().add(AccessTokenImpl.parse(
                " {\"tokenId\":\"_0103\",\"tokenRootId\":\"_01023\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        // will be revoked
        accessTokens.getAccessTokens().add(AccessTokenImpl.parse(
                " {\"tokenId\":\"_0104\",\"tokenRootId\":\"_01024\",\"clientId\":\"foo\",\"audience\":[\"foo\",\"bar\"], \"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        userProfileCache.setSingleEvent(new usernameLookupStrategy().apply(prc), AccessTokens.ENTRY_NAME,
                accessTokens.serialize());
    }

    private void addRefreshTokenEvents() throws JsonMappingException, JsonProcessingException {
        Long currentTime = System.currentTimeMillis() / 1000;
        RefreshTokens refreshTokens = new RefreshTokens();
        // expired
        refreshTokens.getRefreshTokens().add(RefreshTokenImpl.parse(
                " {\"tokenId\":\"_0201\",\"tokenRootId\":\"_02021\",\"clientId\":\"foo\",\"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime - 10) + "} "));
        // valid
        refreshTokens.getRefreshTokens().add(RefreshTokenImpl.parse(
                " {\"tokenId\":\"_0202\",\"tokenRootId\":\"_02022\",\"clientId\":\"foo\",\"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        // valid
        refreshTokens.getRefreshTokens().add(RefreshTokenImpl.parse(
                " {\"tokenId\":\"_0203\",\"tokenRootId\":\"_02023\",\"clientId\":\"foo\",\"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        // will be revoked
        refreshTokens.getRefreshTokens().add(RefreshTokenImpl.parse(
                " {\"tokenId\":\"_0204\",\"tokenRootId\":\"_02024\",\"clientId\":\"foo\",\"scope\":[\"openid\",\"profile\"],\"exp\":"
                        + (currentTime + 10) + "} "));
        userProfileCache.setSingleEvent(new usernameLookupStrategy().apply(prc), RefreshTokens.ENTRY_NAME,
                refreshTokens.serialize());
    }

    public class mockLookUp<ParentContext extends BaseContext, ChildContext extends BaseContext>
            implements ContextDataLookupFunction<ParentContext, ChildContext> {

        @Override
        public ChildContext apply(ParentContext t) {
            return null;
        }

    }

    public class usernameLookupStrategy implements Function<ProfileRequestContext, String> {

        public String apply(final ProfileRequestContext input) {
            return "name";
        }

    }

}
