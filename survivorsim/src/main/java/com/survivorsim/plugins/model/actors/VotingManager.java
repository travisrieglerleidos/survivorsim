package com.survivorsim.plugins.model.actors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.survivorsim.plugins.model.support.GlobalProperty;
import com.survivorsim.plugins.model.support.GroupProperty;
import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.PersonProperty;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ActorContext;
import gov.hhs.aspr.ms.gcm.simulation.plugins.globalproperties.datamanagers.GlobalPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupTypeId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.stochastics.datamanagers.StochasticsDataManager;

public class VotingManager {

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

        actorContext.addPlan(((c) -> voteOffPlayer()), actorContext.getTime() + 2.1);

    }

    private void voteOffPlayer() {
        actorContext.releaseOutput("The vote is about to begin!");

        if (merged) {
            individualVoting();
        } else {
            identifySusceptibleTribes();
        }

        if (gameOver) {
            return;
        }
    
        actorContext.addPlan(((c) -> voteOffPlayer()), actorContext.getTime() + 2);
    }

    private void identifySusceptibleTribes() {

        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);

        for (GroupId tribeGroupId : tribeGroupIds) {
            Boolean isTribeImmune = groupsDataManager.getGroupPropertyValue(tribeGroupId, GroupProperty.IS_IMMUNE);

            if (!isTribeImmune) {
                tribalVoting(tribeGroupId);
            }
        }
    }

    private void tribalVoting(GroupId tribeGroupId) {

        List<PersonId> playerIdsInTribe = groupsDataManager.getPeopleForGroup(tribeGroupId);

        /*
         * If the tribe currently has 4 players and we vote 1 out now, then 3 players will remain.
         * We cannot risk the tribe losing another member next round (2 is too few for a tribe to compete), so we must merge the tribes
         */
        boolean shouldMergeAfterVote = playerIdsInTribe.size() <= 4;

        // Select 1 player to vote off
        Comparator<PersonId> comparingInt = Comparator.comparingInt((PersonId playerId) -> {
            return personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.THREAT_LEVEL);
        }).reversed();

        playerIdsInTribe.sort(comparingInt);

        PersonId playerIdVotedOff = null;

        for (PersonId playerId : playerIdsInTribe) {
            if (randomGenerator.nextInt(100) < 33) {
                playerIdVotedOff = playerId;
                break;
            }
        }

        if (playerIdVotedOff == null) {
            playerIdVotedOff = playerIdsInTribe.get(playerIdsInTribe.size() - 1);
        }

        actorContext.releaseOutput("Player " + playerIdVotedOff + " from tribe " + tribeGroupId + " was voted off!");
        GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.SENT_HOME).get(0);
        transferPlayerToNewGroup(playerIdVotedOff, tribeGroupId, newGroupId);

        // Report the status of each group after the vote:
        Set<GroupTypeId> groupTypeIds = groupsDataManager.getGroupTypeIds();

        for (GroupTypeId groupTypeId : groupTypeIds) {
            List<GroupId> groupsForGroupType = groupsDataManager.getGroupsForGroupType(groupTypeId);
            for (GroupId groupId : groupsForGroupType) {
                actorContext.releaseOutput("GroupType: " + groupTypeId + ", GroupId: " + groupId + " has this many people: " + groupsDataManager.getPersonCountForGroup(groupId));
            }
        }

        if (shouldMergeAfterVote) {
            actorContext.releaseOutput("---------------------------The tribes have merged!---------------------------");
            globalPropertiesDataManager.setGlobalPropertyValue(GlobalProperty.MERGED, true);
            merged = true;
            mergeTribes();
        }

    }

    private void mergeTribes() {
     
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);
        GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.MERGED_TRIBE).get(0);

        for (GroupId tribeGroupId : tribeGroupIds) {
            List<PersonId> playerIdsInTribe = groupsDataManager.getPeopleForGroup(tribeGroupId);
            for (PersonId playerId : playerIdsInTribe) {
                transferPlayerToNewGroup(playerId, tribeGroupId, newGroupId);
            }
        }
    }

    private void individualVoting() {

        GroupId mergedTribeGroupId = groupsDataManager.getGroupsForGroupType(GroupType.MERGED_TRIBE).get(0);
        List<PersonId> playerIdsInMergedTribe = groupsDataManager.getPeopleForGroup(mergedTribeGroupId);

        // If we have 2 players remaining, then this is the last vote and the game should end
        gameOver = playerIdsInMergedTribe.size() <= 2;

        List<PersonId> playersNotSafe = new ArrayList<>();

        for (PersonId playerId : playerIdsInMergedTribe) {
            boolean isImmune = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.IS_IMMUNE);
            actorContext.releaseOutput("Player " + playerId + " is immune? " + isImmune);

            if (!isImmune) {
                playersNotSafe.add(playerId); 
            }
        }

        actorContext.releaseOutput("playersNotSafe has this many members: " + playersNotSafe.size());

        // Select 1 player to vote off
        Comparator<PersonId> comparingInt = Comparator.comparingInt((PersonId playerId) -> {
            return personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.THREAT_LEVEL);
        }).reversed();

        playersNotSafe.sort(comparingInt);

        PersonId playerIdVotedOff = null;

        for (PersonId playerId : playersNotSafe) {
            if (randomGenerator.nextInt(100) < 33) {
                playerIdVotedOff = playerId;
                break;
            }
        }

        if (playerIdVotedOff == null) {
            playerIdVotedOff = playersNotSafe.get(playersNotSafe.size() - 1);
        }

        actorContext.releaseOutput("Oh No! Player " + playerIdVotedOff + " was voted off!!");

        // Move the voted off player from the merged tribe to the jury
        GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.JURY).get(0);
        transferPlayerToNewGroup(playerIdVotedOff, mergedTribeGroupId, newGroupId);

        // Report the status of the merged tribe and jury after the vote:
        int numberOfPlayersInMergedTribe = groupsDataManager.getPeopleForGroup(mergedTribeGroupId).size();
        int numberOfPlayersInJury = groupsDataManager.getPeopleForGroup(newGroupId).size();
        actorContext.releaseOutput("Merged tribe has this many members: " + numberOfPlayersInMergedTribe);
        actorContext.releaseOutput("Jury has this many members: " + numberOfPlayersInJury);

    }

    private void transferPlayerToNewGroup(PersonId playerId, GroupId oldGroupId, GroupId newGroupId) {
        groupsDataManager.removePersonFromGroup(playerId, oldGroupId);
        groupsDataManager.addPersonToGroup(playerId, newGroupId);
    }

}
