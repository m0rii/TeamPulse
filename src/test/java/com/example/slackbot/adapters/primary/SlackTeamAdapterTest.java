package com.example.slackbot.adapters.primary;

import com.example.slackbot.application.TeamService;
import com.example.slackbot.domain.Team;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SlackTeamAdapterTest {

    @Mock
    private App app;

    @Mock
    private TeamService teamService;
    
    @Mock
    private SlashCommandContext commandContext;
    
    @Mock
    private SlashCommandRequest commandRequest;
    
    @Mock
    private SlashCommandPayload commandPayload;

    private SlackTeamAdapter slackTeamAdapter;
    private ArgumentCaptor<SlashCommandHandler> handlerCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        slackTeamAdapter = new SlackTeamAdapter(app, teamService);
        handlerCaptor = ArgumentCaptor.forClass(SlashCommandHandler.class);
        
        // Setup the command context mock
        when(commandRequest.getPayload()).thenReturn(commandPayload);
        when(commandContext.ack(anyString())).thenReturn(new Response());
        
        // Initialize the adapter and capture the command handler
        slackTeamAdapter.init();
        verify(app).command(eq("/team"), handlerCaptor.capture());
    }

    @Test
    void testPromoteUserToManager() throws Exception {
        // Setup: A team with a manager and a regular member
        Team team = new Team();
        team.setId("team1");
        team.setName("Engineering");
        team.addManager("manager1");
        team.addMember("dev1");
        
        when(commandPayload.getText()).thenReturn("promote team1 dev1");
        when(commandPayload.getUserId()).thenReturn("manager1");
        when(teamService.getTeamById("team1")).thenReturn(Optional.of(team));
        
        // Execute the command handler
        Response response = handlerCaptor.getValue().apply(commandRequest, commandContext);
        
        // Verify that the team service was called to update the team
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamService).updateTeam(teamCaptor.capture());
        
        // Verify the manager was promoted
        Team updatedTeam = teamCaptor.getValue();
        assertTrue(updatedTeam.isManager("dev1"));
        assertEquals(2, updatedTeam.getManagerIds().size());
        
        // Verify the success message
        verify(commandContext).ack(contains("Promoted <@dev1> to manager of team: *Engineering*"));
    }
    
    @Test
    void testDemoteManager() throws Exception {
        // Setup: A team with multiple managers
        Team team = new Team();
        team.setId("team1");
        team.setName("Engineering");
        team.addManager("manager1");
        team.addManager("manager2");
        
        when(commandPayload.getText()).thenReturn("demote team1 manager2");
        when(commandPayload.getUserId()).thenReturn("manager1");
        when(teamService.getTeamById("team1")).thenReturn(Optional.of(team));
        
        // Execute the command handler
        Response response = handlerCaptor.getValue().apply(commandRequest, commandContext);
        
        // Verify that the team service was called to update the team
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamService).updateTeam(teamCaptor.capture());
        
        // Verify the manager was demoted
        Team updatedTeam = teamCaptor.getValue();
        assertFalse(updatedTeam.isManager("manager2"));
        assertEquals(1, updatedTeam.getManagerIds().size());
        
        // Verify the success message
        verify(commandContext).ack(contains("Demoted <@manager2> from manager role in team: *Engineering*"));
    }
    
    @Test
    void testCannotDemoteLastManager() throws Exception {
        // Setup: A team with only one manager
        Team team = new Team();
        team.setId("team1");
        team.setName("Engineering");
        team.addManager("manager1");
        team.addMember("dev1");
        
        when(commandPayload.getText()).thenReturn("demote team1 manager1");
        when(commandPayload.getUserId()).thenReturn("manager1");
        when(teamService.getTeamById("team1")).thenReturn(Optional.of(team));
        
        // Execute the command handler
        Response response = handlerCaptor.getValue().apply(commandRequest, commandContext);
        
        // Verify the team service was NOT called to update the team
        verify(teamService, never()).updateTeam(any());
        
        // Verify the error message
        verify(commandContext).ack(contains("Cannot demote the last manager of the team"));
    }
    
    @Test
    void testNonManagerCannotPromote() throws Exception {
        // Setup: A team where the requester is not a manager
        Team team = new Team();
        team.setId("team1");
        team.setName("Engineering");
        team.addManager("manager1");
        team.addMember("dev1");
        team.addMember("dev2");
        
        when(commandPayload.getText()).thenReturn("promote team1 dev2");
        when(commandPayload.getUserId()).thenReturn("dev1"); // Regular member trying to promote
        when(teamService.getTeamById("team1")).thenReturn(Optional.of(team));
        
        // Execute the command handler
        Response response = handlerCaptor.getValue().apply(commandRequest, commandContext);
        
        // Verify the team service was NOT called to update the team
        verify(teamService, never()).updateTeam(any());
        
        // Verify the error message
        verify(commandContext).ack(contains("You must be a manager of the team to promote members"));
    }
    
    private static String contains(String text) {
        return argThat(argument -> argument.contains(text));
    }
} 