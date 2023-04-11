package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

public abstract interface Token {

    public String getTokenId();

    public String getTokenRootId();

    public String getClientId();

    public List<String> getScope();

    public long getExp();

}
