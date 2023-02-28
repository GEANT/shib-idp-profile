package org.geant.shibboleth.plugin.userprofile.storage;

import java.io.IOException;
import java.time.Duration;

import org.opensaml.storage.impl.client.ClientStorageService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import net.minidev.json.JSONArray;
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
    private UsernamePrincipal foo2barUser = new UsernamePrincipal("foo2@bar");

    private JSONObject jsonObject = new JSONObject();

    @BeforeMethod
    protected void setUp() throws Exception {

        storageService = new MemoryStorageService();
        storageService.setId("test");
        storageService.initialize();

        userProfileCache = new UserProfileCache();
        userProfileCache.setRecordExpiration(Duration.ofMillis(500));
        userProfileCache.setStorage(storageService);
        userProfileCache.initialize();

        jsonObject.put("key1", "value1");
        jsonObject.put("key2", 1);

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
        // Test Reading Record
        JSONObject result = userProfileCache.getRecord(foobarUser);
        Assert.assertNotNull(result);
        Assert.assertEquals("value2", ((JSONObject)result.get("name")).getAsString("value"));
        Assert.assertEquals("value3", ((JSONObject)result.get("name2")).getAsString("value"));
        Assert.assertNull(userProfileCache.getRecord(foo2barUser));
        Assert.assertEquals("value2",
                ((JSONObject)userProfileCache.getSingleEvent(foobarUser, "name")).get("value"));
        Assert.assertEquals("value3",
                ((JSONObject)userProfileCache.getSingleEvent(foobarUser, "name2")).get("value"));
        /*
        Assert.assertEquals("value2", result.get("name").getAsJsonObject().get("value").getAsString());
        Assert.assertEquals("value3", result.get("name2").getAsJsonObject().get("value").getAsString());
        Assert.assertNull(userProfileCache.getRecord(foo2barUser));
        // Test Reading Events
        Assert.assertEquals("value2",
                userProfileCache.getSingleEvent(foobarUser, "name").getAsJsonObject().get("value").getAsString());
        Assert.assertEquals("value3",
                userProfileCache.getSingleEvent(foobarUser, "name2").getAsJsonObject().get("value").getAsString());
        */        
        Assert.assertNull(userProfileCache.getSingleEvent(foobarUser, "name3"));
        Assert.assertNull(userProfileCache.getMultiEvent(foobarUser, "name2"));
    }

    @Test
    public void testSingleJsonEvent() throws ComponentInitializationException, IOException {
        Assert.assertTrue(userProfileCache.setSingleEvent(foobarUser, "name", jsonObject));
        // Test Reading Record
        JSONObject result = userProfileCache.getRecord(foobarUser);
        Assert.assertNotNull(result);
        Assert.assertEquals("value1",
                ((JSONObject)((JSONObject)result.get("name")).get("value")).get("key1"));
        Assert.assertEquals(1,
                ((JSONObject)((JSONObject)result.get("name")).get("value")).get("key2"));
        // Test Reading Events
        Assert.assertEquals("value1", ((JSONObject)((JSONObject)userProfileCache.getSingleEvent(foobarUser, "name")).get("value")
                ).get("key1"));
        Assert.assertEquals(1, ((JSONObject)((JSONObject)userProfileCache.getSingleEvent(foobarUser, "name")).get("value")
                ).get("key2"));
        /*
        Assert.assertEquals("value1",
                result.get("name").getAsJsonObject().get("value").getAsJsonObject().get("key1").getAsString());
        Assert.assertEquals(1,
                result.get("name").getAsJsonObject().get("value").getAsJsonObject().get("key2").getAsInt());
        // Test Reading Events
        Assert.assertEquals("value1", userProfileCache.getSingleEvent(foobarUser, "name").getAsJsonObject().get("value")
                .getAsJsonObject().get("key1").getAsString());
        Assert.assertEquals(1, userProfileCache.getSingleEvent(foobarUser, "name").getAsJsonObject().get("value")
                .getAsJsonObject().get("key2").getAsInt());
        */        
    }

    @Test
    public void testMultiEvent() throws ComponentInitializationException, IOException {
        Assert.assertTrue(userProfileCache.addMultiEvent(foobarUser, "name", "value1", 2));
        Assert.assertTrue(userProfileCache.addMultiEvent(foobarUser, "name", "value2", 2));
        Assert.assertTrue(userProfileCache.addMultiEvent(foobarUser, "name", "value3", 2));
        Assert.assertTrue(userProfileCache.addMultiEvent(foobarUser, "name2", "value4", 2));
        // Test Reading Record
        JSONObject result = userProfileCache.getRecord(foobarUser);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, ((JSONArray)result.get("name")).size());
        Assert.assertEquals("value2",
                ((JSONObject)((JSONArray)result.get("name")).get(0)).get("value"));
        Assert.assertEquals("value3",
                ((JSONObject)((JSONArray)result.get("name")).get(1)).get("value"));
        Assert.assertEquals(1, ((JSONArray)result.get("name2")).size());
        Assert.assertEquals("value4",
                ((JSONObject)((JSONArray)result.get("name2")).get(0)).get("value"));
        // Test Reading Events
        Assert.assertEquals(2, userProfileCache.getMultiEvent(foobarUser, "name").size());
        Assert.assertEquals("value2",
                ((JSONObject)userProfileCache.getMultiEvent(foobarUser, "name").get(0)).get("value"));
        Assert.assertEquals("value3",
                ((JSONObject)userProfileCache.getMultiEvent(foobarUser, "name").get(1)).get("value"));
        Assert.assertEquals(1, ((JSONArray)userProfileCache.getMultiEvent(foobarUser, "name2")).size());
        Assert.assertEquals("value4", ((JSONObject)userProfileCache.getMultiEvent(foobarUser, "name2").get(0))
                .get("value"));
        /*
        Assert.assertEquals(2, result.get("name").getAsJsonArray().size());
        Assert.assertEquals("value2",
                result.get("name").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString());
        Assert.assertEquals("value3",
                result.get("name").getAsJsonArray().get(1).getAsJsonObject().get("value").getAsString());
        Assert.assertEquals(1, result.get("name2").getAsJsonArray().size());
        Assert.assertEquals("value4",
                result.get("name2").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString());
        // Test Reading Events
        Assert.assertEquals(2, userProfileCache.getMultiEvent(foobarUser, "name").size());
        Assert.assertEquals("value2",
                userProfileCache.getMultiEvent(foobarUser, "name").get(0).getAsJsonObject().get("value").getAsString());
        Assert.assertEquals("value3",
                userProfileCache.getMultiEvent(foobarUser, "name").get(1).getAsJsonObject().get("value").getAsString());
        Assert.assertEquals(1, userProfileCache.getMultiEvent(foobarUser, "name2").getAsJsonArray().size());
        Assert.assertEquals("value4", userProfileCache.getMultiEvent(foobarUser, "name2").get(0).getAsJsonObject()
                .get("value").getAsString());
        
        */
        Assert.assertNull(userProfileCache.getSingleEvent(foobarUser, "name2"));
    }

}