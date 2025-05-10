package com.example.slackbot.application;

import com.example.slackbot.domain.Team;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TeamService {
    /**
     * Create a new team
     * @param team The team to create
     * @return The created team with ID assigned
     */
    Team createTeam(Team team);
    
    /**
     * Update an existing team
     * @param team The team to update
     * @return The updated team
     */
    Team updateTeam(Team team);
    
    /**
     * Delete a team
     * @param teamId The ID of the team to delete
     */
    void deleteTeam(String teamId);
    
    /**
     * Get a team by its ID
     * @param teamId The ID of the team
     * @return The team, or empty if not found
     */
    Optional<Team> getTeamById(String teamId);
    
    /**
     * Get all teams
     * @return List of all teams
     */
    List<Team> getAllTeams();
    
    /**
     * Get teams that a user belongs to
     * @param userId The ID of the user
     * @return List of teams the user belongs to
     */
    List<Team> getTeamsByUserId(String userId);
    
    /**
     * Check if a user is in a team
     * @param teamId The ID of the team
     * @param userId The ID of the user
     * @return true if the user is in the team, false otherwise
     */
    boolean isUserInTeam(String teamId, String userId);
    
    /**
     * Add a user to a team
     * @param teamId The ID of the team
     * @param userId The ID of the user to add
     */
    void addUserToTeam(String teamId, String userId);
    
    /**
     * Remove a user from a team
     * @param teamId The ID of the team
     * @param userId The ID of the user to remove
     */
    void removeUserFromTeam(String teamId, String userId);
    
    /**
     * Get team members
     * @param teamId The ID of the team
     * @return Set of user IDs of team members
     */
    Set<String> getTeamMembers(String teamId);
} 