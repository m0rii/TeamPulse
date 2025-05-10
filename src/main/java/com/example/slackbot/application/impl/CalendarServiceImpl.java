package com.example.slackbot.application.impl;

import com.example.slackbot.adapters.secondary.OutlookCalendarAdapter;
import com.example.slackbot.application.CalendarService;
import com.example.slackbot.domain.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarServiceImpl implements CalendarService {
    private final OutlookCalendarAdapter outlookCalendarAdapter;
    private final ZoneId timezone;
    
    @Autowired
    public CalendarServiceImpl(
            OutlookCalendarAdapter outlookCalendarAdapter,
            @Value("${scheduling.timezone}") String timezoneName) {
        this.outlookCalendarAdapter = outlookCalendarAdapter;
        this.timezone = ZoneId.of(timezoneName);
    }
    
    @Override
    public List<CalendarEvent> getUserDailyEvents(String userEmail) {
        // Get current date in the configured timezone
        LocalDate today = LocalDate.now(timezone);
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);
        return getUserEvents(userEmail, startOfDay, endOfDay);
    }
    
    @Override
    public List<CalendarEvent> getUserEvents(String userEmail, LocalDateTime start, LocalDateTime end) {
        return outlookCalendarAdapter.getUserEvents(userEmail, start, end);
    }
    
    @Override
    public boolean isUserInMeeting(String userEmail) {
        // Get current time in the configured timezone
        ZonedDateTime now = ZonedDateTime.now(timezone);
        return isUserInMeeting(userEmail, now.toLocalDateTime());
    }
    
    @Override
    public boolean isUserInMeeting(String userEmail, LocalDateTime time) {
        LocalDateTime startWindow = time.minusMinutes(15);
        LocalDateTime endWindow = time.plusMinutes(15);
        
        List<CalendarEvent> events = getUserEvents(userEmail, startWindow, endWindow);
        
        return events.stream()
                .anyMatch(event -> 
                    !event.getStart().isAfter(time) && 
                    !event.getEnd().isBefore(time) &&
                    "Busy".equals(event.getStatus()));
    }
    
    @Override
    public String getUserAvailabilityStatus(String userEmail) {
        // First check if the user is in a meeting
        if (isUserInMeeting(userEmail)) {
            return "In a meeting";
        }
        
        // Then check the presence status from Outlook
        return outlookCalendarAdapter.getUserAvailabilityStatus(userEmail);
    }
    
    @Override
    public CalendarEvent getNextMeeting(String userEmail) {
        // Get current time and end of day in the configured timezone
        ZonedDateTime nowZoned = ZonedDateTime.now(timezone);
        LocalDateTime now = nowZoned.toLocalDateTime();
        
        LocalDate today = LocalDate.now(timezone);
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);
        
        List<CalendarEvent> events = getUserEvents(userEmail, now, endOfDay);
        
        return events.stream()
                .filter(event -> event.getStart().isAfter(now))
                .min((e1, e2) -> e1.getStart().compareTo(e2.getStart()))
                .orElse(null);
    }
    
    @Override
    public Map<String, String> getTeamAvailabilityStatus(List<String> userEmails) {
        Map<String, String> statusMap = new HashMap<>();
        
        for (String email : userEmails) {
            statusMap.put(email, getUserAvailabilityStatus(email));
        }
        
        return statusMap;
    }
} 