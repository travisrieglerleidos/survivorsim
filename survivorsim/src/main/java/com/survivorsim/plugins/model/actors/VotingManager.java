package com.survivorsim.plugins.model.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
        this.groupsDataManager = actorContext.getDataManager(GroupsDataManager.class);
        this.globalPropertiesDataManager = actorContext.getDataManager(GlobalPropertiesDataManager.class);
        this.personPropertiesDataManager = actorContext.getDataManager(PersonPropertiesDataManager.class);
        StochasticsDataManager stochasticsDataManager = actorContext.getDataManager(StochasticsDataManager.class);
		this.randomGenerator = stochasticsDataManager.getRandomGenerator();

        double planTime = actorContext.getTime() + 2.1;
        System.out.println("-----------------planning the next vote for time: " + planTime);
        actorContext.addPlan(((c) -> voteOffPlayer()), planTime);

    }

    private void voteOffPlayer() {
        // Start unnecessary section
        System.out.println("about to start voting off a player!");

        Set<GroupTypeId> groupTypeIds = groupsDataManager.getGroupTypeIds();

        for (GroupTypeId groupTypeId : groupTypeIds) {

            List<GroupId> groupsForGroupType = groupsDataManager.getGroupsForGroupType(groupTypeId);

            for (GroupId groupId : groupsForGroupType) {
                System.out.println("GroupType: " + groupTypeId + ", GroupId: " + groupId + " has this many people: " + groupsDataManager.getPersonCountForGroup(groupId));
            }
            
        }
        // End unnecessary section

        if (!merged) {
            tribeVoting();
        } else {
            individualVoting();
        }

        if (gameOver) {
            System.out.println("Voting manager hit game over!!");
            return;
        }

        double planTime = actorContext.getTime() + 2;
        System.out.println("-----------------planning the next vote for time: " + planTime);
        actorContext.addPlan(((c) -> voteOffPlayer()), planTime);

    }

    private void tribeVoting() {

        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);

        for (GroupId groupId : tribeGroupIds) {

            Boolean isGroupImmune = groupsDataManager.getGroupPropertyValue(groupId, GroupProperty.IS_IMMUNE);

            if (!isGroupImmune) {
                actuallyVoteOffPlayer(groupId);
            }

        }
    }

    private void actuallyVoteOffPlayer(GroupId groupId) {

        List<PersonId> peopleForGroup = groupsDataManager.getPeopleForGroup(groupId);

        boolean shouldMergeAfterVote = peopleForGroup.size() <= 4;

        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(peopleForGroup, random);

        PersonId personId = peopleForGroup.get(0);

        groupsDataManager.removePersonFromGroup(personId, groupId);

        GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.SENT_HOME).get(0);

        groupsDataManager.addPersonToGroup(personId, newGroupId);

        if (shouldMergeAfterVote) {
            globalPropertiesDataManager.setGlobalPropertyValue(GlobalProperty.MERGED, true);
            merged = true;
            mergeTribes();
        }
 
    }

    private void mergeTribes() {
     
        List<GroupId> tribeGroupIds = groupsDataManager.getGroupsForGroupType(GroupType.TRIBE);

        for (GroupId groupId : tribeGroupIds) {

            List<PersonId> peopleForGroup = groupsDataManager.getPeopleForGroup(groupId);

            for (PersonId personId : peopleForGroup) {

                groupsDataManager.removePersonFromGroup(personId, groupId);

                GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.MERGED_TRIBE).get(0);

                groupsDataManager.addPersonToGroup(personId, newGroupId);

            }

        }

    }

    private void individualVoting() {

        GroupId groupId = groupsDataManager.getGroupsForGroupType(GroupType.MERGED_TRIBE).get(0);
        List<PersonId> peopleForGroup = groupsDataManager.getPeopleForGroup(groupId);
        gameOver = peopleForGroup.size() <= 2;

        List<PersonId> peopleNotSafe = new ArrayList<>();

        for (PersonId personId : peopleForGroup) {

            boolean isImmune = personPropertiesDataManager.getPersonPropertyValue(personId, PersonProperty.IS_IMMUNE);
            System.out.println("person " + personId + " is immune? " + isImmune);

            if (!isImmune) {
                peopleNotSafe.add(personId); 
            }

        }

        System.out.println("peopleNotSafe has this many members: " + peopleNotSafe.size());

        Random random = new Random(randomGenerator.nextLong());
        Collections.shuffle(peopleNotSafe, random);

        PersonId personVotedOff = peopleNotSafe.get(0);
        System.out.println("Oh No! Person " + personVotedOff + " was voted off!!");

        groupsDataManager.removePersonFromGroup(personVotedOff, groupId);

        GroupId newGroupId = groupsDataManager.getGroupsForGroupType(GroupType.JURY).get(0);

        groupsDataManager.addPersonToGroup(personVotedOff, newGroupId);

    }

}
