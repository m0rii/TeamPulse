package com.example.slackbot.application.impl;

import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
import com.example.slackbot.application.TeamService;
import com.example.slackbot.domain.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {
    private static final String TEAM_KEY_PREFIX = "team:";
    private static final String ALL_TEAMS_KEY = "all_teams";
    
    private final CloudflareKVAdapter kvAdapter;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TeamServiceImpl(CloudflareKVAdapter kvAdapter, ObjectMapper objectMapper) {
        this.kvAdapter = kvAdapter;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Team createTeam(Team team) {
        // Generate a unique ID if not provided
        if (team.getId() == null || team.getId().isEmpty()) {
            team.setId(UUID.randomUUID().toString());
        }
        
        try {
            // Store the team
            String teamJson = objectMapper.writeValueAsString(team);
            kvAdapter.put(TEAM_KEY_PREFIX + team.getId(), teamJson);
            
            // Update the list of all teams
            updateAllTeamsIndex(team.getId(), true);
            
            return team;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize team", e);
        }
    }
    
    @Override
    public Team updateTeam(Team team) {
        if (team.getId() == null || team.getId().isEmpty()) {
            throw new IllegalArgumentException("Team ID cannot be null or empty");
        }
        
        // Check if team exists
        if (!getTeamById(team.getId()).isPresent()) {
            throw new NoSuchElementException("Team not found: " + team.getId());
        }
        
        try {
            String teamJson = objectMapper.writeValueAsString(team);
            kvAdapter.put(TEAM_KEY_PREFIX + team.getId(), teamJson);
            return team;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize team", e);
        }
    }
    
    @Override
    public void deleteTeam(String teamId) {
        // Remove from the all teams index
        updateAllTeamsIndex(teamId, false);
        
        // Delete the team
        kvAdapter.delete(TEAM_KEY_PREFIX + teamId);
    }
    
    @Override
    public Optional<Team> getTeamById(String teamId) {
        String teamJson = kvAdapter.get(TEAM_KEY_PREFIX + teamId);
        if (teamJson == null || teamJson.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Team team = objectMapper.readValue(teamJson, Team.class);
            return Optional.of(team);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize team", e);
        }
    }
    
    @Override
    public List<Team> getAllTeams() {
        // Retrieve the list of all team IDs
        List<String> teamIds = getTeamIds();
        
        // Retrieve each team
        return teamIds.stream()
            .map(this::getTeamById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Team> getTeamsByUserId(String userId) {
        return getAllTeams().stream()
            .filter(team -> team.isMember(userId) || team.isManager(userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isUserInTeam(String teamId, String userId) {
        Optional<Team> teamOpt = getTeamById(teamId);
        return teamOpt.isPresent() && 
            (teamOpt.get().isMember(userId) || teamOpt.get().isManager(userId));
    }
    
    @Override
    public void addUserToTeam(String teamId, String userId) {
        getTeamById(teamId).ifPresent(team -> {
            team.addMember(userId);
            updateTeam(team);
        });
    }
    
    @Override
    public void removeUserFromTeam(String teamId, String userId) {
        getTeamById(teamId).ifPresent(team -> {
            team.removeMember(userId);
            updateTeam(team);
        });
    }
    
    @Override
    public Set<String> getTeamMembers(String teamId) {
        return getTeamById(teamId)
            .map(Team::getMemberIds)
            .orElse(Collections.emptySet());
    }
    
    // Helper methods
    
    private List<String> getTeamIds() {
        String teamsJson = kvAdapter.get(ALL_TEAMS_KEY);
        if (teamsJson == null || teamsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(teamsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize team IDs", e);
        }
    }
    
    private void updateAllTeamsIndex(String teamId, boolean add) {
        List<String> teamIds = getTeamIds();
        
        if (add && !teamIds.contains(teamId)) {
            teamIds.add(teamId);
        } else if (!add) {
            teamIds.remove(teamId);
        } else {
            // Team ID already exists in the list, no change needed
            return;
        }
        
        try {
            String teamsJson = objectMapper.writeValueAsString(teamIds);
            kvAdapter.put(ALL_TEAMS_KEY, teamsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize team IDs", e);
        }
    }
} 