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
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensaml.storage.StorageCapabilities;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.Positive;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * Stores and returns user profile events.
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
        if (caps instanceof StorageCapabilities) {
            Constraint.isTrue(((StorageCapabilities) caps).isServerSide(), "StorageService cannot be client-side");
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
     * Sets event for user by event name. Overwrites any pre-existing event of same
     * name.
     * 
     * @param user       the user event is stored for
     * @param eventName  name of the event
     * @param eventValue value of the event
     * @return true if event was successfully set
     * @throws IOException
     */
    public synchronized boolean setSingleEvent(@Nonnull @NotEmpty final String user,
            @Nonnull @NotEmpty final String eventName, @Nonnull @NotEmpty final String eventValue) {
        final String key = getKey(user);
        Events events = getEvents(key);
        events.getEvents().put(eventName, new Event(eventValue));
        return setEvents(key, events);
    }

    /**
     * Sets event for user by event name. Overwrites any pre-existing event of same
     * name.
     * 
     * @param eventName   name of the event
     * @param eventValue  value of the event
     * @param eventsCache cache for events
     * @return true if event was successfully set
     * @throws IOException
     */
    public void setSingleEvent(@Nonnull @NotEmpty final String eventName, @Nonnull @NotEmpty final String eventValue,
            @Nonnull EventsCache eventsCache) {
        eventsCache.getEvents().getEvents().put(eventName, new Event(eventValue));
    }

    /**
     * Commit events to storage.
     * 
     * @param user        the user events are stored for
     * @param eventsCache cache for events
     * @return
     */
    public synchronized boolean commitEventsCache(@Nonnull @NotEmpty final String user,
            @Nonnull EventsCache eventsCache) {
        return setEvents(getKey(user), eventsCache.getEvents());
    }

    /**
     * Get event of user by event name.
     * 
     * @param user      the user event is stored for
     * @param eventName name of the event
     * @return event if such exists
     */

    @Nullable
    @NotEmpty
    public synchronized Event getSingleEvent(@Nonnull @NotEmpty final String user,
            @Nonnull @NotEmpty String eventName) {
        Events events = getEvents(getKey(user));
        return events.getEvents().get(eventName);
    }

    /**
     * Get event of user by event name.
     * 
     * @param user        the user event is stored for
     * @param eventName   name of the event
     * @param eventsCache cache for events
     * @return event if such exists
     */
    @Nullable
    @NotEmpty
    public Event getSingleEvent(@Nonnull @NotEmpty final String user, @Nonnull @NotEmpty String eventName,
            @Nonnull EventsCache eventsCache) {
        if (eventsCache.getEvents() == null) {
            eventsCache.setEvents(getEvents(getKey(user)));
        }
        return eventsCache.getEvents().getEvents().get(eventName);
    }

    /**
     * Sets events.
     * 
     * @param key    the key events are stored by.
     * @param events the events to be stored.
     * @return true if operation was success.
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
     * Get events.
     * 
     * @param key the key events are stored by.
     * @return events if such exist.
     */
    @Nonnull
    private synchronized Events getEvents(@Nonnull @NotEmpty final String key) {
        // TODO: Add optional symmetric encryption for record.
        try {
            final StorageRecord<?> entry = storage.read(context, key);
            if (entry == null) {
                log.debug("No User Profile Record for  '{}'", key);
                return new Events();
            }
            log.trace("Located User Profile Record '{}' for user '{}'", entry.getValue(), key);
            return Events.parse(entry.getValue());
        } catch (final IOException e) {
            log.error("Exception reading from storage service, user '{}'. Empty record is created.", e, key);
            return new Events();
        }
    }

    /**
     * Get storage key by user.
     * 
     * @param user user
     * @return key
     */

    @Nonnull
    @NotEmpty
    private String getKey(@Nonnull @NotEmpty final String user) {
        // TODO: Key user with salted hash. Basic precaution.
        final StorageCapabilities caps = storage.getCapabilities();
        if (user.length() > caps.getKeySize()) {
            return DigestUtils.sha1Hex(user);
        } else {
            return user;
        }
    }
}