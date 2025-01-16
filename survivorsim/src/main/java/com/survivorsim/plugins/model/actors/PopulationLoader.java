package com.survivorsim.plugins.model.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupType;

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

    public void init(ActorContext actorContext) {

        // Grab various data managers
        PeopleDataManager peopleDataManager = actorContext.getDataManager(PeopleDataManager.class);
        GlobalPropertiesDataManager globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        GroupsDataManager groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        RegionsDataManager regionsDataManager = actorContext.getDataManager(RegionsDataManager.class);
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		RandomGenerator randomGenerator = stochasticsDataManager.getRandomGenerator();

        // Grab global property values
        int numberOfTribes = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.NUMBER_OF_TRIBES);
        int playersPerTribe = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.PLAYERS_PER_TRIBE);
        int totalPlayers = numberOfTribes * playersPerTribe;

        List<RegionId> regionIds = new ArrayList<>(regionsDataManager.getRegionIds());

        // Create the players
        for (int i = 0; i < totalPlayers; i++) {

            PersonConstructionData personConstructionData = PersonConstructionData.builder()
                .add(regionIds.get(0))
                .build();
            PersonId playerId = peopleDataManager.addPerson(personConstructionData);
            actorContext.releaseOutput("Created player: " + playerId);

        }

        /*
         * Create the groups. If the group type is tribe, create the requested number of tribes. 
         * For all other group types, create only 1 group
         */
        for (GroupTypeId groupTypeId : groupsDataManager.getGroupTypeIds()) {

            GroupConstructionInfo groupConstructionInfo = GroupConstructionInfo.builder()
                .setGroupTypeId(groupTypeId)
                .build();

            if (groupTypeId == GroupType.TRIBE) {
                for (int i = 0; i < numberOfTribes; i++) {
                    groupsDataManager.addGroup(groupConstructionInfo);
                } 
            } else {
                groupsDataManager.addGroup(groupConstructionInfo);
            }

        }

        /*
         * Assign each player to a tribe: 
         * First we shuffle the players, then we grab the tribe group Ids. 
         * Then for each tribe, we add the requested number of players per tribe. 
         * Counter helps ensure we do not assign the same player to multiple tribes
         */
        List<PersonId> playerIds = peopleDataManager.getPeople();
        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(playerIds, random);
        
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);
        actorContext.releaseOutput("We have created this many tribes: " + tribeGroupIds.size());

        int counter = 0;

        for (GroupId tribeGroupId : tribeGroupIds) {

            for (int i = 0; i < playersPerTribe; i++) {
                PersonId playerId = playerIds.get(counter);
                groupsDataManager.addPersonToGroup(playerId, tribeGroupId);
                actorContext.releaseOutput("Added this player to tribe " + tribeGroupId + ": " + playerId);
                counter++;
            }

        }

    }

}
