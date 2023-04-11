package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

public interface LoginEvent {

    public String getRpId();

    public long getTime();

    public List<? extends Attribute> getAttributes();

}
