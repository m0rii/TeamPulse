package com.example.slackbot.adapters.secondary;

import com.example.slackbot.domain.CalendarEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class OutlookCalendarAdapter {
    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String tenantId;
    private final ZoneId timezone;
    private final DateTimeFormatter dateTimeFormatter;
    
    // ISO 8601 format for Microsoft Graph API
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public OutlookCalendarAdapter(
            @Value("${microsoft.clientId}") String clientId,
            @Value("${microsoft.clientSecret}") String clientSecret,
            @Value("${microsoft.tenantId}") String tenantId,
            @Value("${microsoft.timezone}") String timezoneName,
            @Value("${microsoft.dateTimeFormat}") String dateTimeFormat) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.timezone = ZoneId.of(timezoneName);
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        
        this.webClient = WebClient.builder()
                .baseUrl("https://graph.microsoft.com/v1.0")
                .build();
    }
    
    /**
     * Get access token from Microsoft Graph API
     * @return Access token
     */
    private String getAccessToken() {
        // In a real implementation, you would use MSAL or similar library
        // This is a simplified example
        WebClient authClient = WebClient.builder()
                .baseUrl("https://login.microsoftonline.com")
                .build();
                
        String tokenEndpoint = "/" + tenantId + "/oauth2/v2.0/token";
        
        // Request body parameters
        String requestBody = "client_id=" + clientId +
                "&scope=https://graph.microsoft.com/.default" +
                "&client_secret=" + clientSecret +
                "&grant_type=client_credentials";
        
        // Make the request and extract the access token
        return authClient.post()
                .uri(tokenEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    // In a real implementation, parse the JSON response
                    // and extract the access token
                    return "dummy_token";
                })
                .block();
    }
    
    /**
     * Get calendar events for a user within a specific time range
     * @param userEmail The email of the user
     * @param start The start time
     * @param end The end time
     * @return List of calendar events
     */
    public List<CalendarEvent> getUserEvents(String userEmail, LocalDateTime start, LocalDateTime end) {
        String accessToken = getAccessToken();
        
        // Convert LocalDateTime to ZonedDateTime with the configured timezone
        ZonedDateTime zonedStart = start.atZone(ZoneId.systemDefault()).withZoneSameInstant(timezone);
        ZonedDateTime zonedEnd = end.atZone(ZoneId.systemDefault()).withZoneSameInstant(timezone);
        
        // Format the dates according to the configured format
        String startFormatted = zonedStart.format(dateTimeFormatter);
        String endFormatted = zonedEnd.format(dateTimeFormatter);
        
        // Example API call to Microsoft Graph
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{email}/calendarView")
                        .queryParam("startDateTime", startFormatted)
                        .queryParam("endDateTime", endFormatted)
                        .queryParam("$select", "subject,start,end,isAllDay,organizer,location,showAs,onlineMeeting")
                        .build(userEmail))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        // In a real implementation, parse the JSON response and convert to CalendarEvent objects
        // This is a simplified example
        List<CalendarEvent> events = new ArrayList<>();
        
        // Dummy event for demonstration
        CalendarEvent dummyEvent = new CalendarEvent();
        dummyEvent.setId("123");
        dummyEvent.setSubject("Team Meeting");
        dummyEvent.setStart(LocalDateTime.now().plusHours(1));
        dummyEvent.setEnd(LocalDateTime.now().plusHours(2));
        dummyEvent.setAllDay(false);
        dummyEvent.setOrganizerEmail("manager@example.com");
        dummyEvent.setLocation("Conference Room");
        dummyEvent.setStatus("Busy");
        dummyEvent.setOnline(true);
        dummyEvent.setOnlineMeetingUrl("https://teams.microsoft.com/meeting/123");
        
        events.add(dummyEvent);
        
        return events;
    }
    
    /**
     * Get the current availability status of a user
     * @param userEmail The email of the user
     * @return The availability status
     */
    public String getUserAvailabilityStatus(String userEmail) {
        String accessToken = getAccessToken();
        
        // Example API call to Microsoft Graph for presence
        String response = webClient.get()
                .uri("/users/{email}/presence", userEmail)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        // In a real implementation, parse the JSON response and extract the availability status
        // This is a simplified example
        return "Available";
    }
} 