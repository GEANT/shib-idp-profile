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

package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

/**
 * Interface for login event entry stored to user profile storage.
 */
public interface LoginEvent {

    /** Relying party id of the connected service. */
    public String getId();

    /** Name of the connected service. */
    public String getName();

    /** Authentication time as seconds from epoch. */
    public long getTime();
    
    /** Name of the authentication context class principal. */
    public String getAcr();
    
    /** client address. */
    public String getAddress();

    /** Attributes sent. */
    public List<? extends Attribute> getAttributes();

}
