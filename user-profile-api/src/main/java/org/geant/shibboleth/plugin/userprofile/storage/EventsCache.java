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

import javax.annotation.Nullable;

/**
 * Interface for storing {@link UserProfileCache} {@link Events}.
 */
public interface EventsCache {

    /**
     * Set events read from user profile cache.
     * 
     * @param events events read from user profile cache
     */
    public void setEvents(@Nullable Events events);

    /**
     * Get events read from user profile cache.
     * 
     * @return events read from user profile cache
     */
    @Nullable
    public Events getEvents();

}
