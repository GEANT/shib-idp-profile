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

package org.geant.shibboleth.plugin.userprofile.intercept.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.geant.shibboleth.plugin.userprofile.context.UserProfileCacheContext;
import org.geant.shibboleth.plugin.userprofile.event.impl.AccessTokens;
import org.geant.shibboleth.plugin.userprofile.event.impl.RefreshTokens;
import org.geant.shibboleth.plugin.userprofile.storage.UserProfileCache;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.impl.MemoryStorageService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;

import net.shibboleth.idp.plugin.oidc.op.messaging.context.AccessTokenContext;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;
import net.shibboleth.idp.plugin.oidc.op.token.support.AccessTokenClaimsSet;
import net.shibboleth.idp.plugin.oidc.op.token.support.RefreshTokenClaimsSet;
import net.shibboleth.idp.plugin.oidc.op.token.support.TokenClaimsSet;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.DataSealerException;
import net.shibboleth.shared.security.impl.BasicKeystoreKeyStrategy;
import net.shibboleth.shared.spring.resource.ResourceHelper;

/**
 * Unit tests for {@link StoreToken}.
 */
public class StoreTokenTest {

    private DataSealer dataSealer;

    private MemoryStorageService storageService;

    private UserProfileCache userProfileCache;

    private RequestContext src;

    private ProfileRequestContext prc;

    private StoreToken action;

    private UserProfileCacheContext userProfileCacheContext;

    private DataSealer getDataSealer() throws ComponentInitializationException, NoSuchAlgorithmException {
        if (dataSealer == null) {
            dataSealer = initializeDataSealer();
        }
        return dataSealer;
    }

    private static DataSealer initializeDataSealer() throws ComponentInitializationException, NoSuchAlgorithmException {
        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        strategy.setKeystoreResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.jks")));
        strategy.setKeyVersionResource(ResourceHelper.of(new ClassPathResource("credentials/sealer.kver")));
        strategy.setKeystorePassword("password");
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("password");
        strategy.initialize();
        final DataSealer dataSealer = new DataSealer();
        dataSealer.setKeyStrategy(strategy);
        dataSealer.setRandom(SecureRandom.getInstance("SHA1PRNG"));
        dataSealer.initialize();
        return dataSealer;

    }

    @BeforeMethod
    public void initTests() throws ComponentInitializationException, JsonProcessingException, NoSuchAlgorithmException,
            URISyntaxException, DataSealerException {
        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        userProfileCache = new UserProfileCache();
        userProfileCache.setRecordExpiration(Duration.ofMillis(500));
        userProfileCache.setStorage(storageService);
        userProfileCache.setId("id");
        userProfileCache.initialize();
        
        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);

        action = new StoreToken();
        action.setUserProfileCache(userProfileCache);
        action.setUsernameLookupStrategy(new usernameLookupStrategy());
        action.setRelyingPartyIdLookupStrategy(new RelyingPartyIdLookupFunction());

        src = (new RequestContextBuilder()).buildRequestContext();
        prc = (new WebflowRequestContextProfileRequestContextLookup()).apply(this.src);

        RelyingPartyContext relyingPartyContext = (RelyingPartyContext) prc.addSubcontext(new RelyingPartyContext(),
                true);
        relyingPartyContext.setRelyingPartyId("rpId");

        userProfileCacheContext = (UserProfileCacheContext) prc.addSubcontext(new UserProfileCacheContext(), true);
        MessageContext outboundMessageContext = new MessageContext();
        AccessTokenContext accessTokenContext = (AccessTokenContext) outboundMessageContext
                .addSubcontext(new OIDCAuthenticationResponseContext(), true).addSubcontext(new AccessTokenContext());
        prc.setOutboundMessageContext(outboundMessageContext);
        final TokenClaimsSet claims = new AccessTokenClaimsSet.Builder().setJWTID("101").setClientID(new ClientID())
                .setIssuer("issuer").setPrincipal("name").setSubject("subject").setIssuedAt(Instant.now())
                .setExpiresAt(Instant.now().plusSeconds(1)).setAuthenticationTime(Instant.now())
                .setRedirectURI(new URI("http://example.com")).setScope(new Scope()).build();
        // set refresh token
        prc.getOutboundMessageContext().getSubcontext(OIDCAuthenticationResponseContext.class)
                .setRefreshToken(new RefreshTokenClaimsSet.Builder(claims, Instant.now(), Instant.now().plusSeconds(1))
                        .build().serialize(getDataSealer()));
        // set access token
        accessTokenContext.setOpaque(claims.serialize(getDataSealer()));
        action.setDataSealer(getDataSealer());

    }

    @AfterMethod
    protected void tearDown() {
        userProfileCache.destroy();
        userProfileCache = null;

        storageService.destroy();
        storageService = null;

    }

    @Test
    public void testSuccess() throws ComponentInitializationException, JsonMappingException, JsonProcessingException,
            InterruptedException {
        action.initialize();
        Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        userProfileCache.commitEventsCache(new usernameLookupStrategy().apply(null), userProfileCacheContext);
        org.geant.shibboleth.plugin.userprofile.storage.Event events = userProfileCache
                .getSingleEvent(new usernameLookupStrategy().apply(null), AccessTokens.ENTRY_NAME);
        AccessTokens accessToken = AccessTokens.parse(events.getValue());
        Assert.assertEquals(accessToken.getAccessTokens().size(), 1);
        Assert.assertEquals(accessToken.getAccessTokens().get(0).getTokenId(), "101");
        events = userProfileCache.getSingleEvent(new usernameLookupStrategy().apply(null), RefreshTokens.ENTRY_NAME);
        RefreshTokens refreshToken = RefreshTokens.parse(events.getValue());
        Assert.assertEquals(refreshToken.getRefreshTokens().size(), 1);
        Assert.assertEquals(refreshToken.getRefreshTokens().get(0).getTokenId(), "101");

        // Second add
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        userProfileCache.commitEventsCache(new usernameLookupStrategy().apply(null), userProfileCacheContext);
        events = userProfileCache.getSingleEvent(new usernameLookupStrategy().apply(null), AccessTokens.ENTRY_NAME);
        accessToken = AccessTokens.parse(events.getValue());
        Assert.assertEquals(accessToken.getAccessTokens().size(), 2);
        Assert.assertEquals(accessToken.getAccessTokens().get(0).getTokenId(), "101");
        events = userProfileCache.getSingleEvent(new usernameLookupStrategy().apply(null), RefreshTokens.ENTRY_NAME);
        refreshToken = RefreshTokens.parse(events.getValue());
        Assert.assertEquals(refreshToken.getRefreshTokens().size(), 2);
        Assert.assertEquals(refreshToken.getRefreshTokens().get(0).getTokenId(), "101");
    }

    public class usernameLookupStrategy implements Function<ProfileRequestContext, String> {

        public String apply(final ProfileRequestContext input) {
            return "name";
        }

    }
}
