package org.geant.shibboleth.plugin.userprofile;

import java.io.IOException;

import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.idp.module.PropertyDrivenIdPModule;

/**
 * {@link IdPModule} implementation.
 */
public final class UserProfileModule extends PropertyDrivenIdPModule {

    /**
     * Constructor.
     * 
     * @throws ModuleException on error
     * @throws IOException     on error
     */
    public UserProfileModule() throws IOException, ModuleException {
        super(UserProfileModule.class);
    }

}