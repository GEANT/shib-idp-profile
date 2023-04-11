package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

public interface ConnectedOrganization {

    public String getRpId();

    public long getTimes();

    public List<? extends Attribute> getLastAttributes();

}
