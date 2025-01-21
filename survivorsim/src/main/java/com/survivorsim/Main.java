package com.survivorsim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import com.survivorsim.plugins.model.ModelPlugin;
import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupProperty;
import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.ModelReportLabel;
import com.survivorsim.plugins.model.support.PersonProperty;
import com.survivorsim.plugins.model.support.Region;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.Dimension;
import gov.hhs.aspr.ms.gcm.simulation.nucleus.Experiment;
import gov.hhs.aspr.ms.gcm.simulation.nucleus.FunctionalDimension;
import gov.hhs.aspr.ms.gcm.simulation.nucleus.Plugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.GlobalPropertiesPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.GroupsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.reports.GroupPopulationReportPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.PeoplePlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.datamanagers.PeoplePluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.PersonPropertiesPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.properties.support.PropertyDefinition;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.RegionsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.datamanagers.RegionsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.reports.support.NIOReportItemHandler;
import gov.hhs.aspr.ms.gcm.simulation.plugins.reports.support.ReportPeriod;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.StochasticsPlugin;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsPluginData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.support.WellState;

public class Main {

    private final Path outputDirectory;

    private Main(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    private NIOReportItemHandler getNIOReportItemHandler() {
        return NIOReportItemHandler.builder()//
            .addReport(ModelReportLabel.GROUP_POPULATION, outputDirectory.resolve("group_population_report.csv"))//
            .addReport(ModelReportLabel.PLAYER_ENDGAME, outputDirectory.resolve("player_endgame_report.csv"))//
            .build();
    }

    private Plugin getGlobalPropertiesPlugin() {

        // Create Plugin DATA Builder
        GlobalPropertiesPluginData.Builder builder = GlobalPropertiesPluginData.builder();

        // Define the number of players per tribe
        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Integer.class)//
            .setDefaultValue(6)//
            .setPropertyValueMutability(false)//
            .build();

        builder.defineGlobalProperty(GlobalProperty.PLAYERS_PER_TRIBE, propertyDefinition, 0);

        // Define the number of tribes
        propertyDefinition = PropertyDefinition.builder()//
            .setType(Integer.class)//
            .setDefaultValue(3)//
            .setPropertyValueMutability(false)//
            .build();

        builder.defineGlobalProperty(GlobalProperty.NUMBER_OF_TRIBES, propertyDefinition, 0);

        // Define a boolean property to signify if the tribes have merged yet
        propertyDefinition = PropertyDefinition.builder()//
            .setType(Boolean.class)//
            .setDefaultValue(false)//
            .setPropertyValueMutability(true)//
            .build();
    
        builder.defineGlobalProperty(GlobalProperty.MERGED, propertyDefinition, 0);

        // Create the final Plugin DATA
        GlobalPropertiesPluginData globalPropertiesPluginData = builder.build();

        // Create and return the final Plugin
        return GlobalPropertiesPlugin.builder()//
            .setGlobalPropertiesPluginData(globalPropertiesPluginData)//
            .getGlobalPropertiesPlugin();

    }

    private Plugin getStochasticsPlugin() {
        WellState wellState = WellState.builder().setSeed(0).build();
		StochasticsPluginData stochasticsPluginData = StochasticsPluginData.builder()//
			.setMainRNGState(wellState)//
			.build();

		return StochasticsPlugin.getStochasticsPlugin(stochasticsPluginData);
    }

    private Dimension getStochasticsDimension(long seed) {
        FunctionalDimension.Builder builder = FunctionalDimension.builder();

        Random random = new Random(seed);

        List<Long> seedValues = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            seedValues.add(random.nextLong());
        }

        IntStream.range(0, seedValues.size()).forEach((i) -> {
            builder.addLevel((context) -> {
                StochasticsPluginData.Builder stochasticsPluginDataBuilder = context.getPluginDataBuilder(StochasticsPluginData.Builder.class);
                long seedValue = seedValues.get(i);
                WellState wellState = WellState.builder().setSeed(seedValue).build();
                stochasticsPluginDataBuilder.setMainRNGState(wellState);

                ArrayList<String> result = new ArrayList<>();
                result.add(Integer.toString(i));
                result.add(Long.toString(seedValue) + "L");

                return result;
            });
        });

        builder.addMetaDatum("seed index");
        builder.addMetaDatum("seed value");

        return builder.build();

    }

    private Plugin getRegionsPlugin() {
        RegionsPluginData.Builder regionsPluginDataBuilder = RegionsPluginData.builder();

        regionsPluginDataBuilder.addRegion(new Region(0));
		
		RegionsPluginData regionsPluginData = regionsPluginDataBuilder.build();

		return RegionsPlugin.builder()//
            .setRegionsPluginData(regionsPluginData)//
            .getRegionsPlugin();
    }

    private Plugin getGroupsPlugin() {
        
        GroupsPluginData.Builder builder = GroupsPluginData.builder();

        for (GroupType groupType : GroupType.values()) {
            builder.addGroupTypeId(groupType);
        }

        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Boolean.class)//
            .setDefaultValue(false)//
            .build();

        builder.defineGroupProperty(GroupType.TRIBE, GroupProperty.IS_IMMUNE, propertyDefinition);

        GroupsPluginData groupsPluginData = builder.build();

        GroupPopulationReportPluginData groupPopulationReportPluginData = GroupPopulationReportPluginData.builder()//
			.setReportLabel(ModelReportLabel.GROUP_POPULATION)//
			.setReportPeriod(ReportPeriod.DAILY)//
			.build();//
        
        return GroupsPlugin.builder()//
            .setGroupsPluginData(groupsPluginData)//
            .setGroupPopulationReportPluginData(groupPopulationReportPluginData)//
            .getGroupsPlugin();

    }


    private Plugin getPeoplePlugin(){
        PeoplePluginData peoplePluginData = PeoplePluginData.builder().build();
        return PeoplePlugin.getPeoplePlugin(peoplePluginData);
    }

    private Plugin getPersonPropertiesPlugin() {

        PersonPropertiesPluginData.Builder builder = PersonPropertiesPluginData.builder();

        // Set player immunity status
        PropertyDefinition propertyDefinition = PropertyDefinition.builder()//
            .setType(Boolean.class)//
            .setDefaultValue(false)//
            .build();

        builder.definePersonProperty(PersonProperty.IS_IMMUNE, propertyDefinition, 0, false);

         //Set player stats
        propertyDefinition = PropertyDefinition.builder()//
            .setType(Integer.class)//
            .setDefaultValue(0)//
            .build();

        builder.definePersonProperty(PersonProperty.ENDURANCE, propertyDefinition, 0, false);
        builder.definePersonProperty(PersonProperty.PUZZLE_SOLVING, propertyDefinition, 0, false);
        builder.definePersonProperty(PersonProperty.PHYSICALITY, propertyDefinition, 0, false);
        builder.definePersonProperty(PersonProperty.SOCIAL_SKILLS, propertyDefinition, 0, false);
        builder.definePersonProperty(PersonProperty.INTELLECT, propertyDefinition, 0, false);
        builder.definePersonProperty(PersonProperty.THREAT_LEVEL, propertyDefinition, 0, false);

        PersonPropertiesPluginData personPropertiesPluginData = builder.build();

		return PersonPropertiesPlugin.builder()//
            .setPersonPropertiesPluginData(personPropertiesPluginData)//
			.getPersonPropertyPlugin();

    }

    private void execute() {

        Dimension stochasticsDimension = getStochasticsDimension(539847398756272L);

        Experiment.builder()//
            .addPlugin(getGlobalPropertiesPlugin())//
            .addPlugin(getStochasticsPlugin())//
            .addPlugin(getRegionsPlugin())//
            .addPlugin(getGroupsPlugin())//
            .addPlugin(getPeoplePlugin())//
            .addPlugin(getPersonPropertiesPlugin())//
            .addPlugin(ModelPlugin.getModePlugin())//
            .addDimension(stochasticsDimension)//
            .addExperimentContextConsumer(getNIOReportItemHandler())//
            .build()//
            .execute();
    }

    public static void main(String[] args) throws IOException{
        if (args.length == 0) {
            throw new RuntimeException("One output directory argument is required");
        }
        Path outputDirectory = Paths.get(args[0]);
        if (!Files.exists(outputDirectory)) {
            Files.createDirectory(outputDirectory);
        } else {
            if (!Files.isDirectory(outputDirectory)) {
                throw new IOException("Provided path is not a directory");
            }
        }

        new Main(outputDirectory).execute();
    }
}