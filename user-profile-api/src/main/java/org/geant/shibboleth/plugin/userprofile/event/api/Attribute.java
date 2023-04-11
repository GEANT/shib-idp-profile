package org.geant.shibboleth.plugin.userprofile.event.api;

import java.util.List;

public interface Attribute {

    public String getId();

    public String getName();

    public String getDescription();

    public List<String> getValues();
    
    public String getDisplayValue();

}
