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
        Events events = getEvents(key);
        events.getEvents().put(eventName, new Event(eventValue));
        return setEvents(key, events);
    }

    /**
     * 
     * @param user
     * @param eventType
     * @return
     */

    @Nullable
    @NotEmpty
    public synchronized Event getSingleEvent(@Nonnull @NotEmpty final UsernamePrincipal user,
            @Nonnull @NotEmpty String eventType) {
        Events events = getEvents(getKey(user));
        return events.getEvents().get(eventType);
    }

    /**
     * 
     * @param key
     * @param events
     * @return
     */

    private boolean setEvents(@Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final Events events) {
        try {
            if (!storage.update(context, key, events.serialize(), Instant.now().plus(expires).toEpochMilli())) {
                return storage.create(context, key, events.serialize(), Instant.now().plus(expires).toEpochMilli());
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
    private Events getEvents(@Nonnull @NotEmpty final String key) {
        // TODO: Add optional symmetric encryption for record.
        try {
            final StorageRecord<?> entry = storage.read(context, key);
            if (entry == null) {
                log.debug("No User Profile Record for  '{}'", key);
                return new Events();
            }
            log.debug("Located User Profile Record '{}' for user '{}'", entry.getValue(), key);
            return Events.parse(entry.getValue());
        } catch (final IOException e) {
            log.error("Exception reading from storage service, user '{}'. Empty record is created.", e, key);
            return new Events();
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