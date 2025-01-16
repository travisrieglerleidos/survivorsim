package com.survivorsim.plugins.model.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.GlobalProperty;
import com.survivorsim.plugins.model.GroupType;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ActorContext;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupConstructionInfo;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupTypeId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.datamanagers.PeopleDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonConstructionData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.datamanagers.RegionsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.support.RegionId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsDataManager;

public class PopulationLoader {

    private RandomGenerator randomGenerator;

    public void init(ActorContext actorContext) {

        System.out.println();
        System.out.println("new game!!!");

        // Grab various data managers
        PeopleDataManager peopleDataManager = actorContext.getDataManager(PeopleDataManager.class);
        GlobalPropertiesDataManager globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        GroupsDataManager groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        RegionsDataManager regionsDataManager = actorContext.getDataManager(RegionsDataManager.class);
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		randomGenerator = stochasticsDataManager.getRandomGenerator();

        // Grab global property values
        int numberOfTribes = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.NUMBER_OF_TRIBES);
        int playersPerTribe = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.PLAYERS_PER_TRIBE);
        int totalPlayers = numberOfTribes * playersPerTribe;

        List<RegionId> regionIds = new ArrayList<>(regionsDataManager.getRegionIds());

        // Create the people
        for (int i = 0; i < totalPlayers; i++) {

            PersonConstructionData personConstructionData = PersonConstructionData.builder()
                .add(regionIds.get(0))
                .build();
            PersonId personId = peopleDataManager.addPerson(personConstructionData);
            actorContext.releaseOutput("Created person: " + personId);

        }

        // Create the groups
        for (GroupTypeId groupTypeId : groupsDataManager.getGroupTypeIds()) {

            GroupConstructionInfo groupConstructionInfo = GroupConstructionInfo.builder()
                .setGroupTypeId(groupTypeId)
                .build();

            groupsDataManager.addGroup(groupConstructionInfo);

        }

        int remainingTribesToCreate = numberOfTribes - 1;

        for (int i = 0; i < remainingTribesToCreate; i++) {

            GroupConstructionInfo groupConstructionInfo = GroupConstructionInfo.builder()
                .setGroupTypeId(GroupType.TRIBE)
                .build();

            groupsDataManager.addGroup(groupConstructionInfo);

        }


        int counter = 0;
        List<PersonId> people = peopleDataManager.getPeople();
        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(people, random);
        
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);
        System.out.println("group type tribe current has this many groups: " + tribeGroupIds.size());

        for (GroupId tribeGroupId : tribeGroupIds) {

            for (int i = 0; i < playersPerTribe; i++) {

                PersonId personId = people.get(counter);
                groupsDataManager.addPersonToGroup(personId, tribeGroupId);
                actorContext.releaseOutput("added this person to tribe " + tribeGroupId + ": " + personId);
                counter++;

            }

        }

    

    }

}
