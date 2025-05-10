package com.example.slackbot.application;

import com.example.slackbot.domain.CalendarEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CalendarService {
    /**
     * Get all calendar events for a user on the current day
     * @param userEmail The email of the user
     * @return List of calendar events
     */
    List<CalendarEvent> getUserDailyEvents(String userEmail);
    
    /**
     * Get all calendar events for a user within a specific time range
     * @param userEmail The email of the user
     * @param start The start time
     * @param end The end time
     * @return List of calendar events
     */
    List<CalendarEvent> getUserEvents(String userEmail, LocalDateTime start, LocalDateTime end);
    
    /**
     * Check if a user is currently in a meeting
     * @param userEmail The email of the user
     * @return true if the user is in a meeting, false otherwise
     */
    boolean isUserInMeeting(String userEmail);
    
    /**
     * Check if a user is in a meeting at a specific time
     * @param userEmail The email of the user
     * @param time The time to check
     * @return true if the user is in a meeting, false otherwise
     */
    boolean isUserInMeeting(String userEmail, LocalDateTime time);
    
    /**
     * Get the current availability status of a user
     * @param userEmail The email of the user
     * @return The availability status (Available, Busy, Away, etc.)
     */
    String getUserAvailabilityStatus(String userEmail);
    
    /**
     * Get the next meeting for a user
     * @param userEmail The email of the user
     * @return The next calendar event or null if none
     */
    CalendarEvent getNextMeeting(String userEmail);
    
    /**
     * Get the availability status for multiple users
     * @param userEmails List of user emails
     * @return Map of user emails to their availability status
     */
    Map<String, String> getTeamAvailabilityStatus(List<String> userEmails);
} 