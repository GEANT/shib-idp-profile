package org.geant.shibboleth.plugin.userprofile;

import java.io.IOException;
import java.util.Collections;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.plugin.PluginException;
import net.shibboleth.idp.plugin.PropertyDrivenIdPPlugin;

/**
 * Details about the User Profile plugin.
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