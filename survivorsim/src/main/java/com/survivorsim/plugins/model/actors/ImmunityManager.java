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

        // Grab various data managers
        groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        personPropertiesDataManager = actorContext.getDataManager(PersonPropertiesDataManager.class);
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		randomGenerator = stochasticsDataManager.getRandomGenerator();

        //Subscribe to event which states that the tribes have merged
        actorContext.subscribe(globalPropertiesDataManager.getEventFilterForGlobalPropertyUpdateEvent(GlobalProperty.MERGED), this::handleMergedUpdateEvent);
    
        actorContext.addPlan(((c) -> awardImmunity()), actorContext.getTime() + 2);

    }

    private void handleMergedUpdateEvent(final ActorContext actorContext, final GlobalPropertyUpdateEvent globalPropertyUpdateEvent) {
        merged = true;
    }

    public void awardImmunity() {

        actorContext.releaseOutput("The immunity challenge is beginning!");

        if (!merged) {
            awardTribalImmunity();
        } else {
            awardIndividualImmunity();
        }

        if (gameOver) {
            return;
        }

        actorContext.addPlan(((c) -> awardImmunity()), actorContext.getTime() + 2);
        
    }

    private void awardTribalImmunity() {
        /*
         * Technically, all tribes start the challenge without immunity and then every tribe
         * but the losing tribe gains immunity back. 
         * 
         * But it is more efficient to make every tribe immune
         * and then select 1 tribe to lose immunity.
         */

        // Add immunity to all tribes
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);

        for (GroupId groupId : tribeGroupIds) {
            groupsDataManager.setGroupPropertyValue(groupId, GroupProperty.IS_IMMUNE, true);
        }
                
        int indexOfTribeLosingImmunity = randomGenerator.nextInt(tribeGroupIds.size());
        GroupId tribeLosingImmunity = tribeGroupIds.get(indexOfTribeLosingImmunity);
        actorContext.releaseOutput("Tribe " + tribeLosingImmunity + " lost immunity!");
        groupsDataManager.setGroupPropertyValue(tribeLosingImmunity, GroupProperty.IS_IMMUNE, false);
  
    }

    private void awardIndividualImmunity() {
 
        // Remove immunity from all players
        List<PersonId> playerIds = groupsDataManager.getPeopleForGroupType(GroupType.MERGED_TRIBE);

        /*
         * If we have 2 players remaining, then this is the last immunity that we need to award. 
         * In that case, we do not want to plan another immunity
         */
        int playersLeft = playerIds.size();
        if (playersLeft <= 2) {
            gameOver = true;
        }

        for (PersonId playerId : playerIds) {
            personPropertiesDataManager.setPersonPropertyValue(playerId, PersonProperty.IS_IMMUNE, false);
        }

        // Select a player to award immunity to
        int indexOfPlayerWinningImmunity = randomGenerator.nextInt(playersLeft);
        PersonId playerWinningImmunity = playerIds.get(indexOfPlayerWinningImmunity);
        actorContext.releaseOutput("Player " + playerWinningImmunity + " won immunity!");
        personPropertiesDataManager.setPersonPropertyValue(playerWinningImmunity, PersonProperty.IS_IMMUNE, true);

    }

}
