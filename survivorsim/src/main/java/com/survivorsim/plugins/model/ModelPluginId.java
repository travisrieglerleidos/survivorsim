package com.survivorsim.plugins.model;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.PluginId;
import gov.hhs.aspr.ms.gcm.simulation.nucleus.SimplePluginId;

public final class ModelPluginId {

    private ModelPluginId() {
    }

    public final static PluginId PLUGIN_ID = new SimplePluginId("model plugin");
    
}
