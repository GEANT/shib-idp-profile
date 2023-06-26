package org.geant.shibboleth.plugin.userprofile.context;

import org.geant.shibboleth.plugin.userprofile.storage.Events;
import org.geant.shibboleth.plugin.userprofile.storage.EventsCache;
import org.opensaml.messaging.context.BaseContext;

public class UserProfileCacheContext extends BaseContext implements EventsCache {

    /** Cached user profile events. */
    private Events cachedEvents;

    /** {@inheritDoc} */
    @Override
    public void setEvents(Events events) {
        cachedEvents = events;
    }

    /** {@inheritDoc} */
    @Override
    public Events getEvents() {
        return cachedEvents;
    }

}
