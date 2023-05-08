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
 * Interface for oauth2 tokens stored to user profile storage.
 */
public abstract interface Token {

    /** Token identifier. */
    public String getTokenId();

    /** Token root identifier. */
    public String getTokenRootId();

    /** Client Id of the relying party token is generated for. */
    public String getClientId();

    /** Token scope. */
    public List<String> getScope();

    /** Token expiration time as seconds from epoch. */
    public long getExp();

}
