package com.example.slackbot.application.impl;

import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
import com.example.slackbot.domain.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeamServiceTest {

    @Mock
    private CloudflareKVAdapter kvAdapter;

    private TeamServiceImpl teamService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        teamService = new TeamServiceImpl(kvAdapter, objectMapper);
        
        // Setup initial team data
        List<String> allTeamIds = Arrays.asList("team1", "team2");
        when(kvAdapter.get("all_teams")).thenReturn(objectMapper.writeValueAsString(allTeamIds));
        
        // Team 1 with single manager
        Team team1 = new Team();
        team1.setId("team1");
        team1.setName("Engineering Team");
        team1.setDescription("Team for engineers");
        team1.addManager("user1");
        team1.addMember("user2");
        team1.addMember("user3");
        when(kvAdapter.get("team:team1")).thenReturn(objectMapper.writeValueAsString(team1));
        
        // Team 2 with multiple managers
        Team team2 = new Team();
        team2.setId("team2");
        team2.setName("Design Team");
        team2.setDescription("Team for designers");
        team2.addManager("user4");
        team2.addManager("user5");
        team2.addMember("user6");
        when(kvAdapter.get("team:team2")).thenReturn(objectMapper.writeValueAsString(team2));
    }

    @Test
    void testGetTeamById() {
        Optional<Team> team = teamService.getTeamById("team1");
        assertTrue(team.isPresent());
        assertEquals("Engineering Team", team.get().getName());
        assertEquals(1, team.get().getManagerIds().size());
        assertTrue(team.get().isManager("user1"));
    }

    @Test
    void testCreateTeamWithManager() {
        Team newTeam = new Team();
        newTeam.setName("QA Team");
        newTeam.setDescription("Team for QA engineers");
        newTeam.addManager("user7");
        
        Team createdTeam = teamService.createTeam(newTeam);
        
        assertNotNull(createdTeam.getId());
        assertEquals("QA Team", createdTeam.getName());
        assertEquals(1, createdTeam.getManagerIds().size());
        assertTrue(createdTeam.isManager("user7"));
        assertTrue(createdTeam.isMember("user7")); // Manager should also be a member
    }

    @Test
    void testAddingMultipleManagers() throws JsonProcessingException {
        Team team = teamService.getTeamById("team1").get();
        team.addManager("user3");
        teamService.updateTeam(team);
        
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kvAdapter).put(eq("team:team1"), valueCaptor.capture());
        
        Team updatedTeam = objectMapper.readValue(valueCaptor.getValue(), Team.class);
        assertEquals(2, updatedTeam.getManagerIds().size());
        assertTrue(updatedTeam.isManager("user1"));
        assertTrue(updatedTeam.isManager("user3"));
    }

    @Test
    void testRemovingManager() throws JsonProcessingException {
        // Get team with multiple managers
        Team team = teamService.getTeamById("team2").get();
        assertEquals(2, team.getManagerIds().size());
        
        // Remove one manager
        team.removeManager("user5");
        teamService.updateTeam(team);
        
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kvAdapter).put(eq("team:team2"), valueCaptor.capture());
        
        Team updatedTeam = objectMapper.readValue(valueCaptor.getValue(), Team.class);
        assertEquals(1, updatedTeam.getManagerIds().size());
        assertTrue(updatedTeam.isManager("user4"));
        assertFalse(updatedTeam.isManager("user5"));
        assertTrue(updatedTeam.isMember("user5")); // Should still be a member
    }

    @Test
    void testIsUserInTeam() {
        assertTrue(teamService.isUserInTeam("team1", "user1")); // Manager is in team
        assertTrue(teamService.isUserInTeam("team1", "user2")); // Regular member is in team
        assertFalse(teamService.isUserInTeam("team1", "user4")); // User not in team
    }

    @Test
    void testGetTeamsByUserId() {
        // Test for a manager
        List<Team> teams = teamService.getTeamsByUserId("user1");
        assertEquals(1, teams.size());
        assertEquals("team1", teams.get(0).getId());
        
        // Test for a member
        teams = teamService.getTeamsByUserId("user2");
        assertEquals(1, teams.size());
        assertEquals("team1", teams.get(0).getId());
    }

    @Test
    void testGetTeamMembers() {
        Set<String> members = teamService.getTeamMembers("team1");
        assertEquals(3, members.size());
        assertTrue(members.contains("user1")); // Manager
        assertTrue(members.contains("user2")); // Regular member
        assertTrue(members.contains("user3")); // Regular member
    }

    @Test
    void testAddAndRemoveUserFromTeam() throws JsonProcessingException {
        // Add user to team
        teamService.addUserToTeam("team1", "user4");
        
        ArgumentCaptor<String> addValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kvAdapter, atLeastOnce()).put(eq("team:team1"), addValueCaptor.capture());
        
        Team teamAfterAdd = objectMapper.readValue(addValueCaptor.getValue(), Team.class);
        assertTrue(teamAfterAdd.isMember("user4"));
        
        // Reset mock to verify next operation cleanly
        reset(kvAdapter);
        when(kvAdapter.get("team:team1")).thenReturn(objectMapper.writeValueAsString(teamAfterAdd));
        
        // Remove user from team
        teamService.removeUserFromTeam("team1", "user4");
        
        ArgumentCaptor<String> removeValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kvAdapter, atLeastOnce()).put(eq("team:team1"), removeValueCaptor.capture());
        
        Team teamAfterRemove = objectMapper.readValue(removeValueCaptor.getValue(), Team.class);
        assertFalse(teamAfterRemove.isMember("user4"));
    }
} 