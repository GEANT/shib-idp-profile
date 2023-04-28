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

package org.geant.shibboleth.plugin.userprofile.storage;

import java.io.IOException;
import java.time.Duration;

import org.opensaml.storage.impl.client.ClientStorageService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import net.minidev.json.JSONObject;

import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.opensaml.storage.impl.MemoryStorageService;

/**
 * Tests for {@link UserProfileCache}
 */
public class UserProfileCacheTest {

    private MemoryStorageService storageService;

    private UserProfileCache userProfileCache;

    private UsernamePrincipal foobarUser = new UsernamePrincipal("foo@bar");

    @BeforeMethod
    protected void setUp() throws Exception {

        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        userProfileCache = new UserProfileCache();
        userProfileCache.setRecordExpiration(Duration.ofMillis(500));
        userProfileCache.setStorage(storageService);
        userProfileCache.initialize();
    }

    @AfterMethod
    protected void tearDown() {
        userProfileCache.destroy();
        userProfileCache = null;

        storageService.destroy();
        storageService = null;
    }

    @Test
    public void testInit() {
        userProfileCache = new UserProfileCache();
        try {
            userProfileCache.setStorage(null);
            Assert.fail("Null StorageService should have caused constraint violation");
        } catch (final Exception e) {
        }

        try {
            userProfileCache.setStorage(new ClientStorageService());
            Assert.fail("ClientStorageService should have caused constraint violation");
        } catch (final Exception e) {
        }
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testExpirationSetter() throws ComponentInitializationException {
        // Must be positive
        userProfileCache.setRecordExpiration(Duration.ZERO);
    }

    @Test
    public void testStorageGetter() throws ComponentInitializationException {
        Assert.assertEquals(storageService, userProfileCache.getStorage());
    }

    @Test
    public void testSingleEvent() throws ComponentInitializationException, IOException {
        Assert.assertTrue(userProfileCache.setSingleEvent(foobarUser, "name", "value1"));
        Assert.assertTrue(userProfileCache.setSingleEvent(foobarUser, "name", "value2"));
        Assert.assertTrue(userProfileCache.setSingleEvent(foobarUser, "name2", "value3"));
        Assert.assertEquals(userProfileCache.getSingleEvent(foobarUser, "name").getValue(), "value2");
        Assert.assertEquals(userProfileCache.getSingleEvent(foobarUser, "name2").getValue(), "value3");
    }

}