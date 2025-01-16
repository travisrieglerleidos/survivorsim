package com.survivorsim.plugins.model.actors;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupProperty;
import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.PersonProperty;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ActorContext;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.events.GlobalPropertyUpdateEvent;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsDataManager;

public class ImmunityManager {

    private ActorContext actorContext;
    private RandomGenerator randomGenerator;
    private GroupsDataManager groupsDataManager;
    private GlobalPropertiesDataManager globalPropertiesDataManager;
    private PersonPropertiesDataManager personPropertiesDataManager;
    private boolean merged = false;
    private boolean gameOver = false;

    public void init(ActorContext actorContext) {

        this.actorContext = actorContext;
        groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        personPropertiesDataManager = actorContext.getDataManager(PersonPropertiesDataManager.class);
        
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		randomGenerator = stochasticsDataManager.getRandomGenerator();

        actorContext.subscribe(globalPropertiesDataManager.getEventFilterForGlobalPropertyUpdateEvent(GlobalProperty.MERGED), this::handleMergedUpdateEvent);

        double planTime = actorContext.getTime() + 2;
        System.out.println("-----------------Scheduling next immunity challenge for time: " + planTime);
        actorContext.addPlan(((c) -> awardImmunity()), planTime);

    }

    private void handleMergedUpdateEvent(final ActorContext actorContext, final GlobalPropertyUpdateEvent globalPropertyUpdateEvent) {
        merged = true;
    }

    public void awardImmunity() {
        System.out.println();
        System.out.println("Entered immunity...");

        if (!merged) {
            awardTribalImmunity();
        } else {
            awardIndividualImmunity();
        }

        if (gameOver) {
            return;
        }

        double planTime = actorContext.getTime() + 2;
        System.out.println("-----------------Scheduling next immunity challenge for time: " + planTime);
        actorContext.addPlan(((c) -> awardImmunity()), planTime);
        
    }

    private void awardTribalImmunity() {

        System.out.println("its a tribal immunity challenge!");
        //remove immunity from all tribes
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);

        for (GroupId groupId : tribeGroupIds) {
            groupsDataManager.setGroupPropertyValue(groupId, GroupProperty.IS_IMMUNE, false);
        }
                
        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(tribeGroupIds, random);
        
        groupsDataManager.setGroupPropertyValue(tribeGroupIds.get(0), GroupProperty.IS_IMMUNE, true);
        groupsDataManager.setGroupPropertyValue(tribeGroupIds.get(1), GroupProperty.IS_IMMUNE, true);

    }

    private void awardIndividualImmunity() {

        System.out.println("its an individual immunity challenge!");
        //remove immunity from all people
        List<PersonId> peopleForGroupType = groupsDataManager.getPeopleForGroupType(GroupType.MERGED_TRIBE);
        int peopleLeft = peopleForGroupType.size();

        if (peopleLeft <= 2) {
            gameOver = true;
        }

        for (PersonId personId : peopleForGroupType) {
            personPropertiesDataManager.setPersonPropertyValue(personId, PersonProperty.IS_IMMUNE, false);
        }

        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(peopleForGroupType, random);

        System.out.println();
        System.out.println("Player " + peopleForGroupType.get(0) + " won immunity!");

        personPropertiesDataManager.setPersonPropertyValue(peopleForGroupType.get(0), PersonProperty.IS_IMMUNE, true);

    }

}
