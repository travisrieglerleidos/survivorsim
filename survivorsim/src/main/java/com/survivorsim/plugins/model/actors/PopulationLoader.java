package com.survivorsim.plugins.model.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.PersonProperty;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ActorContext;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupConstructionInfo;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupTypeId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.datamanagers.PeopleDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonConstructionData;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.support.PersonPropertyValueInitialization;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.datamanagers.RegionsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.regions.support.RegionId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsDataManager;

public class PopulationLoader {

    RegionsDataManager regionsDataManager;
    private RandomGenerator randomGenerator;

    public void init(ActorContext actorContext) {

        // Grab various data managers
        PeopleDataManager peopleDataManager = actorContext.getDataManager(PeopleDataManager.class);
        GlobalPropertiesDataManager globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        GroupsDataManager groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        regionsDataManager = actorContext.getDataManager(RegionsDataManager.class);
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		randomGenerator = stochasticsDataManager.getRandomGenerator();
        PersonPropertiesDataManager personPropertiesDataManager = actorContext.getDataManager(PersonPropertiesDataManager.class);

        // Grab global property values
        int numberOfTribes = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.NUMBER_OF_TRIBES);
        int playersPerTribe = globalPropertiesDataManager.getGlobalPropertyValue(GlobalProperty.PLAYERS_PER_TRIBE);
        int totalPlayers = numberOfTribes * playersPerTribe;

        // Create the players
        for (int i = 0; i < totalPlayers; i++) {

            PersonConstructionData personConstructionData = buildPlayerStats();

            PersonId playerId = peopleDataManager.addPerson(personConstructionData);
            actorContext.releaseOutput(
                "Created player " + playerId + " with these stats:" +
                " endurance=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.ENDURANCE) + 
                " puzzle solving=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.PUZZLE_SOLVING) + 
                " physicality=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.PHYSICALITY) + 
                " social skills=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.SOCIAL_SKILLS) + 
                " intellect=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.INTELLECT) + 
                " threat level=" + personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.THREAT_LEVEL)
            );

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

    private PersonConstructionData buildPlayerStats() {

        List<RegionId> regionIds = new ArrayList<>(regionsDataManager.getRegionIds());

        /*
         * The five core stats start with a value of 7.
         * We then randomly move a point between the core stats five different times.
         * Finally, threat level is determined based on the five core stats
         */
        int[] stats = {7, 7, 7, 7, 7};

        for (int i = 0; i < 5; i++) {
            int indexToGainPoint = randomGenerator.nextInt(stats.length);
            stats[indexToGainPoint]++;

            int indexToLosePoint = randomGenerator.nextInt(stats.length);
            stats[indexToLosePoint]--;
        }

        int endurance = stats[0];
        int puzzleSolving  = stats[1];
        int physicality = stats[2];
        int socialSkills = stats[3];
        int intellect = stats[4];

        int threatlevel = (physicality / 2) + socialSkills + intellect;

        PersonPropertyValueInitialization endurancePropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.ENDURANCE, endurance);
        PersonPropertyValueInitialization puzzleSolvingPropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.PUZZLE_SOLVING, puzzleSolving);
        PersonPropertyValueInitialization physicalityPropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.PHYSICALITY, physicality);
        PersonPropertyValueInitialization socialSkillsPropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.SOCIAL_SKILLS, socialSkills);
        PersonPropertyValueInitialization intellectPropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.INTELLECT, intellect);
        PersonPropertyValueInitialization threatLevelPropertyInitialization = new PersonPropertyValueInitialization(PersonProperty.THREAT_LEVEL, threatlevel);

        return PersonConstructionData.builder()
            .add(regionIds.get(0))
            .add(endurancePropertyInitialization)
            .add(puzzleSolvingPropertyInitialization)
            .add(physicalityPropertyInitialization)
            .add(socialSkillsPropertyInitialization)
            .add(intellectPropertyInitialization)
            .add(threatLevelPropertyInitialization)
            .build();
    }

}
