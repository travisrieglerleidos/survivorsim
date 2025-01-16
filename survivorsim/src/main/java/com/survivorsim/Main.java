package com.survivorsim;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.ModelPlugin;
import com.survivorsim.plugins.model.reports.OutputConsumer;
import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupProperty;
import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.PersonProperty;
import com.survivorsim.plugins.model.support.Region;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.Experiment;
import gov.hhs.aspr.ms.gcm.simulation.nucleus.Plugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.GlobalPropertiesPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.GroupsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.PeoplePlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.datamanagers.PeoplePluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.PersonPropertiesPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.properties.support.PropertyDefinition;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.RegionsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.datamanagers.RegionsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.StochasticsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.support.WellState;
import gov.hhs.aspr.ms.util.random.RandomGeneratorProvider;

public class Main {

    private RandomGenerator randomGenerator = RandomGeneratorProvider.getRandomGenerator(9032703880551658180L);

    private Plugin getPeoplePlugin(){
        PeoplePluginData peoplePluginData = PeoplePluginData.builder().build();
        return PeoplePlugin.getPeoplePlugin(peoplePluginData);
    }

    private Plugin getGlobalPropertiesPlugin() {

        // Create Plugin DATA Builder
        GlobalPropertiesPluginData.Builder builder = GlobalPropertiesPluginData.builder();

        // Define a property
        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Integer.class)//
            .setDefaultValue(6)//
            .setPropertyValueMutability(false)//
            .build();

        builder.defineGlobalProperty(GlobalProperty.PLAYERS_PER_TRIBE, propertyDefinition, 0);

        // Define a second property
        propertyDefinition = PropertyDefinition.builder()//
            .setType(Integer.class)//
            .setDefaultValue(3)//
            .setPropertyValueMutability(false)//
            .build();

        builder.defineGlobalProperty(GlobalProperty.NUMBER_OF_TRIBES, propertyDefinition, 0);

        // Define a third property
        propertyDefinition = PropertyDefinition.builder()//
                .setType(Boolean.class)//
                .setDefaultValue(false)//
                .setPropertyValueMutability(true)//
                .build();
    
        builder.defineGlobalProperty(GlobalProperty.MERGED, propertyDefinition, 0);

        // Create the final Plugin DATA
        GlobalPropertiesPluginData globalPropertiesPluginData = builder.build();

        // Create and return the final Plugin
        return GlobalPropertiesPlugin.builder()
            .setGlobalPropertiesPluginData(globalPropertiesPluginData)
            .getGlobalPropertiesPlugin();

    }

    private Plugin getGroupsPlugin() {
        
        GroupsPluginData.Builder builder = GroupsPluginData.builder();

        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Boolean.class)//
            .setDefaultValue(true)//
            .build();

        for (GroupType groupType : GroupType.values()) {
            builder.addGroupTypeId(groupType);
        }

        builder.defineGroupProperty(GroupType.TRIBE, GroupProperty.IS_IMMUNE, propertyDefinition);

        GroupsPluginData groupsPluginData = builder.build();
        
        return GroupsPlugin.builder()//
            .setGroupsPluginData(groupsPluginData)//
            .getGroupsPlugin();

    }

    private Plugin getStochasticsPlugin() {
        
        WellState wellState = WellState.builder().setSeed(randomGenerator.nextLong()).build();
		StochasticsPluginData stochasticsPluginData = StochasticsPluginData.builder()//
			.setMainRNGState(wellState)//
			.build();

		return StochasticsPlugin.getStochasticsPlugin(stochasticsPluginData);

    }

    private Plugin getPersonPropertiesPlugin() {

        PersonPropertiesPluginData.Builder builder = PersonPropertiesPluginData.builder();

        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Boolean.class)//
            .setDefaultValue(false)//
            .build();

        builder.definePersonProperty(PersonProperty.IS_IMMUNE, propertyDefinition, 0, false);

        PersonPropertiesPluginData personPropertiesPluginData = builder.build();

		return PersonPropertiesPlugin.builder()//
            .setPersonPropertiesPluginData(personPropertiesPluginData)//
			.getPersonPropertyPlugin();

    }

    private Plugin getRegionsPlugin() {
        RegionsPluginData.Builder regionsPluginDataBuilder = RegionsPluginData.builder();

        regionsPluginDataBuilder.addRegion(new Region(0));
		
		RegionsPluginData regionsPluginData = regionsPluginDataBuilder.build();

		return RegionsPlugin.builder()//
            .setRegionsPluginData(regionsPluginData)//
            .getRegionsPlugin();
    }

    private void execute() {
        System.out.println("Hello world!");
        
        Experiment.builder()//
            .addPlugin(getPeoplePlugin())//
            .addPlugin(getRegionsPlugin())//
            .addPlugin(getPersonPropertiesPlugin())//
            .addPlugin(getGroupsPlugin())//
            .addPlugin(getGlobalPropertiesPlugin())//
            .addPlugin(getStochasticsPlugin())
            .addPlugin(ModelPlugin.getModePlugin())//
            .addExperimentContextConsumer(new OutputConsumer())//
            .build()//
            .execute();//
    }

    public static void main(String[] args) {
        new Main().execute();
    }
}