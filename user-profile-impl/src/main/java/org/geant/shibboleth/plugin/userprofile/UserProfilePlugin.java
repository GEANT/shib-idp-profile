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

package org.geant.shibboleth.plugin.userprofile;

import java.io.IOException;
import java.util.Collections;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.plugin.PluginException;
import net.shibboleth.idp.plugin.PropertyDrivenIdPPlugin;

/**
 * Details about the user profile plugin.
 */
public class UserProfilePlugin extends PropertyDrivenIdPPlugin {

    /**
     * Constructor.
     * 
     * @throws IOException     if the properties fail to load
     * @throws PluginException if other errors occur
     */
    public UserProfilePlugin() throws IOException, PluginException {
        super(UserProfilePlugin.class);
        try {
            final IdPModule module = new UserProfileModule();
            setEnableOnInstall(Collections.singleton(module));
            setDisableOnRemoval(Collections.singleton(module));
        } catch (final IOException e) {
            throw e;
        } catch (final ModuleException e) {
            throw new PluginException(e);
        }
    }

}