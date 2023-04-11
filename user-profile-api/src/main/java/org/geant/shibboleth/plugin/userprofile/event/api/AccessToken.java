package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

public interface AccessToken extends Token {

    public List<String> getAudience();

}
