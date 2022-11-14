package org.geant.shibboleth.plugin.userprofile.storage;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.storage.StorageCapabilities;
import org.opensaml.storage.StorageCapabilitiesEx;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Stores and returns user profile related authentication records.
 * 
 * <p>
 * This class is thread-safe and uses a synchronized method to prevent race
 * conditions within the underlying store (lacking an atomic "check and insert"
 * operation).
 * </p>
 * 
 */
@ThreadSafeAfterInit
public class UserProfileCache extends AbstractIdentifiableInitializableComponent {

    /** Logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(UserProfileCache.class);

    /** Context for storage */
    @Nonnull
    @NotEmpty
    final private String context = UserProfileCache.class.getCanonicalName();

    /** Backing storage for the user profile authentication events. */
    @NonnullAfterInit
    private StorageService storage;

    /**
     * Lifetime of user profile authentication events. The lifetime starts from last
     * update to user profile record. Defaults to 180 days.
     */
    @Nonnull
    @Positive
    private Duration expires;

    /**
     * Constructor.
     */
    public UserProfileCache() {
        expires = Duration.ofDays(180);
    }

    /**
     * Set the default user profile authentication record expiration.
     * 
     * @param eventExpiration lifetime of an user profile authentication record in
     *                        milliseconds
     */
    public void setRecordExpiration(@Positive final Duration recordExpiration) {
        Constraint.isTrue(recordExpiration != null && !recordExpiration.isNegative() && !recordExpiration.isZero(),
                "Revocation cache default entry expiration must be greater than 0");
        expires = recordExpiration;
    }

    /**
     * Get the backing store for the cache.
     * 
     * @return the backing store.
     */
    @NonnullAfterInit
    public StorageService getStorage() {
        return storage;
    }

    /**
     * Set the backing store for the cache.
     * 
     * @param storageService backing store to use
     */
    public void setStorage(@Nonnull final StorageService storageService) {
        storage = Constraint.isNotNull(storageService, "StorageService cannot be null");
        final StorageCapabilities caps = storage.getCapabilities();
        if (caps instanceof StorageCapabilitiesEx) {
            Constraint.isTrue(((StorageCapabilitiesEx) caps).isServerSide(), "StorageService cannot be client-side");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doInitialize() throws ComponentInitializationException {
        if (storage == null) {
            throw new ComponentInitializationException("StorageService cannot be null");
        }
    }

    /**
     * 
     * @param user
     * @param eventName
     * @param eventValue
     * @return
     * @throws IOException
     */
    public synchronized boolean setSingleEvent(@Nonnull @NotEmpty final UsernamePrincipal user,
            @Nonnull @NotEmpty final String eventName, @Nonnull @NotEmpty final String eventValue) {
        final String key = getKey(user);
        JsonObject record = getRecord(key);
        record.add(eventName, createJsonEvent(eventValue));
        return setRecord(key, record);
    }

    /**
     * 
     * @param user
     * @param eventName
     * @param eventValue
     * @param maxItems
     * @return
     * @throws IOException
     */

    public synchronized boolean addMultiEvent(@Nonnull @NotEmpty final UsernamePrincipal user,
            @Nonnull @NotEmpty final String eventName, @Nonnull @NotEmpty final String eventValue, int maxItems) {
        Constraint.isTrue(maxItems > 0, "maxItems bust be greater than 0");
        final String key = getKey(user);
        JsonObject record = getRecord(key);
        JsonElement eventElement = record.get(eventName);
        JsonArray eventArray = eventElement != null
                ? eventElement.isJsonArray() ? eventElement.getAsJsonArray() : new JsonArray()
                : new JsonArray();
        eventArray.add(createJsonEvent(eventValue));
        while (eventArray.size() > maxItems) {
            eventArray.remove(0);
        }
        record.add(eventName, eventArray);
        return setRecord(key, record);
    }

    /**
     * 
     * @param user
     * @return
     */

    @Nullable
    @NotEmpty
    public synchronized JsonObject getRecord(@Nonnull @NotEmpty final UsernamePrincipal user) {
        JsonObject record = getRecord(getKey(user));
        return record.size() > 0 ? record : null;

    }

    /**
     * 
     * @param user
     * @param eventType
     * @return
     */

    @Nullable
    @NotEmpty
    public synchronized JsonObject getSingleEvent(@Nonnull @NotEmpty final UsernamePrincipal user,
            @Nonnull @NotEmpty String eventType) {
        JsonObject record = getRecord(getKey(user));
        if (record != null && record.get(eventType) != null && record.get(eventType).isJsonObject()) {
            return record.get(eventType).getAsJsonObject();
        }
        return null;
    }

    /**
     * 
     * @param user
     * @param eventName
     * @return
     */

    @Nullable
    @NotEmpty
    public synchronized JsonArray getMultiEvent(@Nonnull @NotEmpty final UsernamePrincipal user,
            @Nonnull @NotEmpty String eventName) {
        JsonObject record = getRecord(getKey(user));
        if (record != null && record.get(eventName) != null && record.get(eventName).isJsonArray()) {
            return record.get(eventName).getAsJsonArray();
        }
        return null;
    }

    /**
     * 
     * @param value
     * @return
     */
    
    private JsonObject createJsonEvent(String value) {
        JsonObject entry=new JsonObject();
        entry.addProperty("value", value);
        entry.addProperty("iat", Instant.now().getEpochSecond());
        return entry;
    }
    
    /**
     * 
     * @param key
     * @param record
     * @return
     */

    private boolean setRecord(@Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final JsonObject record) {
        try {
            if (!storage.update(context, key, record.toString(), Instant.now().plus(expires).toEpochMilli())) {
                return storage.create(context, key, record.toString(), Instant.now().plus(expires).toEpochMilli());
            }
            return true;
        } catch (final Exception e) {
            log.error("Exception reading/writing to storage service", e);
            return false;
        }
    }

    /**
     * 
     * @param key
     * @return
     */

    @Nonnull
    private JsonObject getRecord(@Nonnull @NotEmpty final String key) {
        // TODO: Add optional symmetric encryption for record.
        try {
            final StorageRecord<?> entry = storage.read(context, key);
            if (entry == null) {
                log.debug("No User Profile Record for  '{}'", key);
                return new JsonObject();
            }
            log.debug("Located User Profile Record '{}' for user '{}'", entry.getValue(), key);
            return JsonParser.parseString(entry.getValue()).getAsJsonObject();
        } catch (final IOException | JsonSyntaxException e) {
            log.error("Exception reading from storage service, user '{}'. Empty record is created.", e, key);
            return new JsonObject();
        }
    }

    /**
     * 
     * @param user
     * @return
     */

    @Nonnull
    @NotEmpty
    private String getKey(@Nonnull @NotEmpty final UsernamePrincipal user) {
        // TODO: Key user with salted hash. Basic precaution.
        final StorageCapabilities caps = storage.getCapabilities();
        if (user.getName().length() > caps.getKeySize()) {
            return DigestUtils.sha1Hex(user.getName());
        } else {
            return user.getName();
        }

    }

}