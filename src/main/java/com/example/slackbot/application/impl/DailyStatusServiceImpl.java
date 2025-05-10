package com.example.slackbot.application.impl;

import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.application.TeamService;
import com.example.slackbot.domain.DailyStatus;
import com.example.slackbot.domain.Team;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyStatusServiceImpl implements DailyStatusService {
    private static final String STATUS_TEAM_PREFIX = "status_team:";
    
    private final CloudflareKVAdapter cloudflareKVAdapter;
    private final TeamService teamService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DailyStatusServiceImpl(
            CloudflareKVAdapter cloudflareKVAdapter,
            TeamService teamService,
            ObjectMapper objectMapper) {
        this.cloudflareKVAdapter = cloudflareKVAdapter;
        this.teamService = teamService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addDailyStatus(DailyStatus status) {
        cloudflareKVAdapter.storeDailyStatus(status);
    }

    @Override
    public List<DailyStatus> getDailyStatuses(String date) {
        return cloudflareKVAdapter.retrieveDailyStatuses(date);
    }
    
    @Override
    public List<DailyStatus> getTeamDailyStatuses(String date, String teamId) {
        // Get all statuses for the date
        List<DailyStatus> allStatuses = getDailyStatuses(date);
        
        // Get team members
        Set<String> teamMembers = teamService.getTeamMembers(teamId);
        
        // Filter statuses to only include team members
        return allStatuses.stream()
                .filter(status -> teamMembers.contains(status.getDeveloperId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasViewPermission(String viewerId, String targetUserId) {
        if (viewerId.equals(targetUserId)) {
            // Users can always view their own status
            return true;
        }
        
        // Get all teams the viewer is in
        List<Team> viewerTeams = teamService.getTeamsByUserId(viewerId);
        
        // Check if the target user is in any of the viewer's teams
        for (Team team : viewerTeams) {
            if (team.isMember(targetUserId) || team.isManager(viewerId)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void associateStatusWithTeam(String statusId, String teamId) {
        try {
            String key = STATUS_TEAM_PREFIX + statusId;
            String existingTeamsJson = cloudflareKVAdapter.get(key);
            
            List<String> teamIds;
            if (existingTeamsJson != null && !existingTeamsJson.isEmpty()) {
                teamIds = objectMapper.readValue(existingTeamsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } else {
                teamIds = new ArrayList<>();
            }
            
            if (!teamIds.contains(teamId)) {
                teamIds.add(teamId);
                String updatedTeamsJson = objectMapper.writeValueAsString(teamIds);
                cloudflareKVAdapter.put(key, updatedTeamsJson);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process team association", e);
        }
    }
} 