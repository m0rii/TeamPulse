package com.example.slackbot.application;

import com.example.slackbot.domain.DailyStatus;
import java.util.List;

public interface DailyStatusService {
    /**
     * Add a daily status for a user
     * @param status The daily status to add
     */
    void addDailyStatus(DailyStatus status);
    
    /**
     * Get all daily statuses for a specific date across all teams
     * @param date The date in format YYYY-MM-DD
     * @return List of daily statuses
     */
    List<DailyStatus> getDailyStatuses(String date);
    
    /**
     * Get daily statuses for a specific date and team
     * @param date The date in format YYYY-MM-DD
     * @param teamId The ID of the team
     * @return List of daily statuses for the team
     */
    List<DailyStatus> getTeamDailyStatuses(String date, String teamId);
    
    /**
     * Check if a user has permission to view another user's status
     * @param viewerId The ID of the user trying to view the status
     * @param targetUserId The ID of the user whose status is being viewed
     * @return true if the viewer has permission, false otherwise
     */
    boolean hasViewPermission(String viewerId, String targetUserId);
    
    /**
     * Associate a status with a team
     * @param statusId The ID of the status
     * @param teamId The ID of the team
     */
    void associateStatusWithTeam(String statusId, String teamId);
} 