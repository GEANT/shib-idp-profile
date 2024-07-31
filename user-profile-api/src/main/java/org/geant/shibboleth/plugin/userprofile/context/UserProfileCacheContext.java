package org.geant.shibboleth.plugin.userprofile.context;

import org.geant.shibboleth.plugin.userprofile.storage.Events;
import org.geant.shibboleth.plugin.userprofile.storage.EventsCache;
import org.opensaml.messaging.context.BaseContext;

public class UserProfileCacheContext extends BaseContext implements EventsCache {

    /** Cached user profile events. */
    private Events cachedEvents;

    /** Name of the authentication context class principal. */
    private String acr;

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

    /**
     * Get name of the authentication context class principal.
     * 
     * @return Name of the authentication context class principal
     */
    public String getAuthnContextClassReferencePrincipalName() {
        return acr;
    }

    /**
     * Set name of the authentication context class principal.
     * 
     * @param name Name of the authentication context class principal
     */
    public void setAuthnContextClassReferencePrincipalName(String name) {
        acr = name;
    }

}
