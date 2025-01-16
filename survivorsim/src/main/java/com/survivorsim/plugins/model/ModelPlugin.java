package com.survivorsim.plugins.model;

import com.survivorsim.plugins.model.actors.ImmunityManager;
import com.survivorsim.plugins.model.actors.PopulationLoader;
import com.survivorsim.plugins.model.actors.VotingManager;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.Plugin;

public final class ModelPlugin {
    private ModelPlugin() {

    }

    public static Plugin getModePlugin() {
        return Plugin.builder()//
            .setPluginId(ModelPluginId.PLUGIN_ID)//
            .setInitializer((c) -> {
                c.addActor(new PopulationLoader()::init);//
                c.addActor(new VotingManager()::init);//
                c.addActor(new ImmunityManager()::init);//
            })//
            .build();
    }
    
}
