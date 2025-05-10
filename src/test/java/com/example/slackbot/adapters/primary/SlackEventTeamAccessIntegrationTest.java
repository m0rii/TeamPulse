package com.example.slackbot.adapters.primary;

import com.example.slackbot.IntegrationTestConfig;
import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.application.TeamService;
import com.example.slackbot.adapters.secondary.OutlookCalendarAdapter;
import com.example.slackbot.domain.DailyStatus;
import com.example.slackbot.domain.Team;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.MethodsClient;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(classes = IntegrationTestConfig.class)
@ActiveProfiles("integrationTest")
class SlackEventTeamAccessIntegrationTest {

    @MockBean
    private App app;
    
    @MockBean
    private DailyStatusService dailyStatusService;
    
    @MockBean
    private TeamService teamService;

    private SlackEventAdapter slackEventAdapter;
    private ArgumentCaptor<SlashCommandHandler> handlerCaptor;

    @BeforeEach
    void setUp() {
        // Mock the dependencies
        dailyStatusService = Mockito.mock(DailyStatusService.class);
        teamService = Mockito.mock(TeamService.class);
        
        // Capture the command handler
        handlerCaptor = ArgumentCaptor.forClass(SlashCommandHandler.class);
        
        // Create the adapter
        slackEventAdapter = new SlackEventAdapter(app, dailyStatusService, teamService);
        
        // Initialize the adapter, which registers the command handlers
        slackEventAdapter.init();
        
        // Verify and capture the command handler for /status
        verify(app).command(eq("/status"), handlerCaptor.capture());
        
        // Mock common responses
        mockResponses();
    }
    
    private void mockResponses() {
        // Set up test teams
        Team engineering = new Team();
        engineering.setId("team1");
        engineering.setName("Engineering");
        engineering.addManager("manager1");
        engineering.addMember("dev1");
        engineering.addMember("dev2");
        
        Team design = new Team();
        design.setId("team2");
        design.setName("Design");
        design.addManager("manager2");
        design.addMember("designer1");
        
        // Mock team service responses
        when(teamService.getTeamById("team1")).thenReturn(Optional.of(engineering));
        when(teamService.getTeamById("team2")).thenReturn(Optional.of(design));
        
        List<Team> manager1Teams = Arrays.asList(engineering);
        List<Team> dev1Teams = Arrays.asList(engineering);
        List<Team> manager2Teams = Arrays.asList(design);
        
        when(teamService.getTeamsByUserId("manager1")).thenReturn(manager1Teams);
        when(teamService.getTeamsByUserId("dev1")).thenReturn(dev1Teams);
        when(teamService.getTeamsByUserId("manager2")).thenReturn(manager2Teams);
        
        // Mock daily status responses
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        List<DailyStatus> allStatuses = new ArrayList<>();
        DailyStatus dev1Status = createStatus("dev1", "Available", "Working on feature X", "Need help with Y");
        DailyStatus dev2Status = createStatus("dev2", "Busy", "Working on feature Z", null);
        DailyStatus designerStatus = createStatus("designer1", "Available", "Creating mockups", null);
        allStatuses.addAll(Arrays.asList(dev1Status, dev2Status, designerStatus));
        
        when(dailyStatusService.getDailyStatuses(today)).thenReturn(allStatuses);
        
        List<DailyStatus> team1Statuses = Arrays.asList(dev1Status, dev2Status);
        List<DailyStatus> team2Statuses = Arrays.asList(designerStatus);
        
        when(dailyStatusService.getTeamDailyStatuses(eq(today), eq("team1"))).thenReturn(team1Statuses);
        when(dailyStatusService.getTeamDailyStatuses(eq(today), eq("team2"))).thenReturn(team2Statuses);
        
        // Mock permission checks
        when(dailyStatusService.hasViewPermission("manager1", "dev1")).thenReturn(true);
        when(dailyStatusService.hasViewPermission("manager1", "dev2")).thenReturn(true);
        when(dailyStatusService.hasViewPermission("manager1", "designer1")).thenReturn(false);
        when(dailyStatusService.hasViewPermission("dev1", "dev2")).thenReturn(true);
        when(dailyStatusService.hasViewPermission("manager2", "designer1")).thenReturn(true);
        when(dailyStatusService.hasViewPermission("manager2", "dev1")).thenReturn(false);
    }
    
    private DailyStatus createStatus(String userId, String availability, String tasks, String notes) {
        DailyStatus status = new DailyStatus();
        status.setDeveloperId(userId);
        status.setAvailability(availability);
        status.setTasks(tasks);
        status.setNotes(notes);
        status.setDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        return status;
    }

    @Test
    void testStatusForTeam() throws Exception {
        // Mock the request and context for manager viewing their team
        SlashCommandRequest request = mockSlashCommandRequest("team team1", "manager1");
        SlashCommandContext context = mockSlashCommandContext();
        
        // Call the handler
        Response response = handlerCaptor.getValue().apply(request, context);
        
        // Verify the response contains team members' statuses
        verify(context).ack(Mockito.argThat((String text) -> 
            text.contains("*Team Engineering Status Summary:*") &&
            text.contains("<@dev1>") &&
            text.contains("<@dev2>")
        ));
    }
    
    @Test
    void testCannotViewOtherTeam() throws Exception {
        // Manager1 trying to view team2's status
        SlashCommandRequest request = mockSlashCommandRequest("team team2", "manager1");
        SlashCommandContext context = mockSlashCommandContext();
        
        // Call the handler
        Response response = handlerCaptor.getValue().apply(request, context);
        
        // Verify access denied message
        verify(context).ack(Mockito.argThat((String text) -> 
            text.contains("You don't have permission to view this team's status")
        ));
    }
    
    @Test
    void testCanViewTeamMemberStatus() throws Exception {
        // Manager viewing a team member's status
        SlashCommandRequest request = mockSlashCommandRequest("dev1", "manager1");
        SlashCommandContext context = mockSlashCommandContext();
        
        // Call the handler
        Response response = handlerCaptor.getValue().apply(request, context);
        
        // Verify can see the status
        verify(context).ack(Mockito.argThat((String text) -> 
            text.contains("*Status for <@dev1>:*") &&
            text.contains("*Availability:* Available") &&
            text.contains("*Tasks:* Working on feature X")
        ));
    }
    
    @Test
    void testCannotViewUserFromOtherTeam() throws Exception {
        // Manager1 trying to view designer1's status (who is in team2)
        SlashCommandRequest request = mockSlashCommandRequest("designer1", "manager1");
        SlashCommandContext context = mockSlashCommandContext();
        
        // Call the handler
        Response response = handlerCaptor.getValue().apply(request, context);
        
        // Verify access denied message
        verify(context).ack(Mockito.argThat((String text) -> 
            text.contains("You don't have permission to view this user's status")
        ));
    }
    
    @Test
    void testEmptyStatusShowsAllTeams() throws Exception {
        // User viewing all their teams' statuses
        SlashCommandRequest request = mockSlashCommandRequest("", "dev1");
        SlashCommandContext context = mockSlashCommandContext();
        
        // Call the handler
        Response response = handlerCaptor.getValue().apply(request, context);
        
        // Verify response shows team summary
        verify(context).ack(Mockito.argThat((String text) -> 
            text.contains("*Your Teams Status Summary:*") &&
            text.contains("*Team: Engineering*")
        ));
    }

    // Helper methods to create mocks
    
    private SlashCommandRequest mockSlashCommandRequest(String text, String userId) {
        SlashCommandRequest request = mock(SlashCommandRequest.class);
        SlashCommandPayload payload = mock(SlashCommandPayload.class);
        
        when(request.getPayload()).thenReturn(payload);
        when(payload.getText()).thenReturn(text);
        when(payload.getUserId()).thenReturn(userId);
        
        return request;
    }
    
    private SlashCommandContext mockSlashCommandContext() {
        SlashCommandContext context = mock(SlashCommandContext.class);
        MethodsClient methodsClient = mock(MethodsClient.class);
        
        when(context.client()).thenReturn(methodsClient);
        when(context.ack(anyString())).thenReturn(new Response());
        
        return context;
    }
} 