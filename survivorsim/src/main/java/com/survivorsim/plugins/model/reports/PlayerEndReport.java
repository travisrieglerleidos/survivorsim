package com.survivorsim.plugins.model.reports;

import java.util.HashMap;
import java.util.Map;

import com.survivorsim.plugins.model.support.GroupType;
import com.survivorsim.plugins.model.support.PersonProperty;

import gov.hhs.aspr.ms.gcm.simulation.nucleus.ReportContext;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.datamanagers.GroupsDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.events.GroupMembershipAdditionEvent;
import gov.hhs.aspr.ms.gcm.simulation.plugins.groups.support.GroupId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.events.PersonAdditionEvent;
import gov.hhs.aspr.ms.gcm.simulation.plugins.people.support.PersonId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.datamanagers.PersonPropertiesDataManager;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.events.PersonPropertyUpdateEvent;
import gov.hhs.aspr.ms.gcm.simulation.plugins.personproperties.support.PersonPropertyId;
import gov.hhs.aspr.ms.gcm.simulation.plugins.reports.support.ReportHeader;
import gov.hhs.aspr.ms.gcm.simulation.plugins.reports.support.ReportItem;
import gov.hhs.aspr.ms.gcm.simulation.plugins.reports.support.ReportLabel;

public final class PlayerEndReport {

    private final ReportHeader reportHeader = ReportHeader.builder()//
        .add("Player Id")//
        .add("Endurance")//
        .add("Puzzle Solving")//
        .add("Physicality")//
        .add("Social Skills")//
        .add("Intellect")//
        .add("Individual Immunity Wins")
        .add("Threat Level")//
        .add("Voted Off Day")//
        .build();

    private final ReportLabel reportLabel;
    private Map<PersonId, PlayerStats> playerIdToPlayerStats = new HashMap<>();

    public PlayerEndReport(ReportLabel reportLabel) {
        this.reportLabel = reportLabel;
    }

    public void init(ReportContext reportContext) {
        reportContext.subscribe(PersonAdditionEvent.class, this::handleNewPlayer);
        reportContext.subscribe(GroupMembershipAdditionEvent.class, this::handleMembershipAddition);
        reportContext.subscribe(PersonPropertyUpdateEvent.class, this::handlePlayerImmunityUpdate);
        reportContext.subscribeToSimulationClose(this::report);
    }

    private void report(ReportContext reportContext) {

        PersonPropertiesDataManager personPropertiesDataManager = reportContext.getDataManager(PersonPropertiesDataManager.class);

        for (PersonId playerId : playerIdToPlayerStats.keySet()) {

            Integer playerEndurance = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.ENDURANCE);
            Integer playerPuzzleSolving = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.PUZZLE_SOLVING);
            Integer playerPhysicality = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.PHYSICALITY);
            Integer playerSocialSkills = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.SOCIAL_SKILLS);
            Integer playerIntellect = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.INTELLECT);
            Integer playerThreatLevel = personPropertiesDataManager.getPersonPropertyValue(playerId, PersonProperty.THREAT_LEVEL);

            ReportItem.Builder reportItemBuilder = ReportItem.builder();
            reportItemBuilder.setReportLabel(reportLabel);
            reportItemBuilder.setReportHeader(reportHeader);
            reportItemBuilder.addValue(playerId);
            reportItemBuilder.addValue(playerEndurance);
            reportItemBuilder.addValue(playerPuzzleSolving);
            reportItemBuilder.addValue(playerPhysicality);
            reportItemBuilder.addValue(playerSocialSkills);
            reportItemBuilder.addValue(playerIntellect);
            reportItemBuilder.addValue(playerIdToPlayerStats.get(playerId).getIndividualImmunityWins());
            reportItemBuilder.addValue(playerThreatLevel);

            Double dayVotedOff = playerIdToPlayerStats.get(playerId).getVotedOffDay();

            if (dayVotedOff == 0) {
                reportItemBuilder.addValue("Winner!");
            } else {
                reportItemBuilder.addValue(dayVotedOff);
            }

            ReportItem reportItem = reportItemBuilder.build();
            reportContext.releaseOutput(reportItem);
        }

    }

    private void handleMembershipAddition(ReportContext reportContext, GroupMembershipAdditionEvent groupMembershipAdditionEvent) {

        GroupsDataManager groupsDataManager = reportContext.getDataManager(GroupsDataManager.class);
        GroupId juryGroupId = groupsDataManager.getGroupsForGroupType(GroupType.JURY).get(0);
        GroupId sentHomeGroupId = groupsDataManager.getGroupsForGroupType(GroupType.SENT_HOME).get(0);

        if (groupMembershipAdditionEvent.groupId() == juryGroupId || groupMembershipAdditionEvent.groupId() == sentHomeGroupId) {

            PersonId playerId = groupMembershipAdditionEvent.personId();
            PlayerStats playerStats = playerIdToPlayerStats.get(playerId);
            Double simulationTime = reportContext.getTime();
            playerStats.setVotedOffDay(simulationTime);

            playerIdToPlayerStats.put(playerId, playerStats);
        }

    }

    private void handleNewPlayer(ReportContext reportContext, PersonAdditionEvent personAdditionEvent) {
        PersonId playerId = personAdditionEvent.personId();
        PlayerStats playerStats = new PlayerStats();
        playerIdToPlayerStats.put(playerId, playerStats);
    }

    private void handlePlayerImmunityUpdate(ReportContext reportContext, PersonPropertyUpdateEvent personPropertyUpdateEvent) {

        PersonPropertyId personPropertyId = personPropertyUpdateEvent.personPropertyId();

        if (personPropertyId == PersonProperty.IS_IMMUNE) {

            Boolean is_Immune = personPropertyUpdateEvent.getCurrentPropertyValue();

            if (is_Immune) {
                PersonId playerId = personPropertyUpdateEvent.personId();
                PlayerStats playerStats = playerIdToPlayerStats.get(playerId);
                Integer individualImmunityWins = playerStats.getIndividualImmunityWins() + 1;
                playerStats.setIndividualImmunityWins(individualImmunityWins);
            }

        }

    }

    private class PlayerStats {
        Double votedOffDay = 0.0;
        Integer individualImmunityWins = 0;

        private PlayerStats() {
        }

        private Double getVotedOffDay() {
            return votedOffDay;
        }

        private Integer getIndividualImmunityWins() {
            return individualImmunityWins;
        }

        private void setVotedOffDay(Double votedOffDay) {
            this.votedOffDay = votedOffDay;
        }

        private void setIndividualImmunityWins(Integer individualImmunityWins) {
            this.individualImmunityWins = individualImmunityWins;
        }

    }
    
}
