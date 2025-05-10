package com.example.slackbot.adapters.primary;

import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.application.TeamService;
import com.example.slackbot.domain.DailyStatus;
import com.example.slackbot.domain.Team;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SlackEventAdapter {
    private final App app;
    private final DailyStatusService dailyStatusService;
    private final TeamService teamService;

    @Autowired
    public SlackEventAdapter(App app, DailyStatusService dailyStatusService, TeamService teamService) {
        this.app = app;
        this.dailyStatusService = dailyStatusService;
        this.teamService = teamService;
    }

    @PostConstruct
    public void init() {
        // Handle app mention events
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            ctx.say("Hello, I am your Slack bot!");
            return ctx.ack();
        });

        // Handle button click to open the status modal
        app.blockAction("set_status", (req, ctx) -> {
            String triggerId = req.getPayload().getTriggerId();
            
            ViewsOpenResponse viewsOpenResponse = ctx.client().viewsOpen(r -> r
                    .triggerId(triggerId)
                    .view(createSimpleStatusModal())
            );
            
            return ctx.ack();
        });

        // Handle modal submission
        app.viewSubmission("status_submission", (req, ctx) -> {
            Map<String, Map<String, com.slack.api.model.view.ViewState.Value>> values = req.getPayload().getView().getState().getValues();
            
            String userId = req.getPayload().getUser().getId();
            String availability = values.get("availability_block").get("availability_action").getSelectedOption().getValue();
            String tasks = values.get("tasks_block").get("tasks_action").getValue();
            String notes = values.get("notes_block").get("notes_action").getValue();
            
            DailyStatus status = new DailyStatus();
            status.setDeveloperId(userId);
            status.setAvailability(availability);
            status.setTasks(tasks);
            status.setNotes(notes);
            status.setDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            
            dailyStatusService.addDailyStatus(status);
            
            // Associate the status with the user's teams
            List<Team> userTeams = teamService.getTeamsByUserId(userId);
            for (Team team : userTeams) {
                dailyStatusService.associateStatusWithTeam(userId, team.getId());
            }
            
            return ctx.ack();
        });

        // Handle slash command for all status
        app.command("/status", (req, ctx) -> {
            String text = req.getPayload().getText();
            String userId = req.getPayload().getUserId();
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            
            if (text.isEmpty()) {
                // Show status for all teams the user is part of
                List<Team> userTeams = teamService.getTeamsByUserId(userId);
                
                if (userTeams.isEmpty()) {
                    return ctx.ack("You are not a member of any team. Join a team first or create one with '/team create'.");
                }
                
                StringBuilder response = new StringBuilder("*Your Teams Status Summary:*\n\n");
                
                for (Team team : userTeams) {
                    response.append("*Team: ").append(team.getName()).append("*\n");
                    List<DailyStatus> teamStatuses = dailyStatusService.getTeamDailyStatuses(date, team.getId());
                    response.append(createStatusSummaryText(teamStatuses));
                    response.append("\n");
                }
                
                return ctx.ack(response.toString());
            } else if (text.startsWith("team ")) {
                // Show status for a specific team
                String teamId = text.substring(5).trim();
                Optional<Team> teamOpt = teamService.getTeamById(teamId);
                
                if (!teamOpt.isPresent()) {
                    return ctx.ack("Team not found with ID: " + teamId);
                }
                
                Team team = teamOpt.get();
                
                // Check if user has permission to view this team's status
                if (!team.isMember(userId) && !team.isManager(userId)) {
                    return ctx.ack("You don't have permission to view this team's status.");
                }
                
                List<DailyStatus> teamStatuses = dailyStatusService.getTeamDailyStatuses(date, teamId);
                String response = "*Team " + team.getName() + " Status Summary:*\n\n" + createStatusSummaryText(teamStatuses);
                
                return ctx.ack(response);
            } else {
                // Assume it's a user ID and check permissions
                String targetUserId = text.trim();
                
                if (!dailyStatusService.hasViewPermission(userId, targetUserId)) {
                    return ctx.ack("You don't have permission to view this user's status.");
                }
                
                List<DailyStatus> statuses = dailyStatusService.getDailyStatuses(date);
                List<DailyStatus> filteredStatuses = new ArrayList<>();
                
                for (DailyStatus status : statuses) {
                    if (status.getDeveloperId().equals(targetUserId)) {
                        filteredStatuses.add(status);
                        break;
                    }
                }
                
                if (filteredStatuses.isEmpty()) {
                    return ctx.ack("<@" + targetUserId + "> has not submitted a status update today.");
                }
                
                return ctx.ack("*Status for <@" + targetUserId + ">:*\n\n" + createStatusSummaryText(filteredStatuses));
            }
        });
    }

    private View createSimpleStatusModal() {
        return View.builder()
                .type("modal")
                .callbackId("status_submission")
                .title(viewTitle("Daily Status"))
                .submit(viewSubmit("Submit"))
                .close(viewClose("Cancel"))
                .blocks(createSimpleModalBlocks())
                .build();
    }
    
    private com.slack.api.model.view.ViewTitle viewTitle(String title) {
        return com.slack.api.model.view.ViewTitle.builder()
                .type("plain_text")
                .text(title)
                .build();
    }
    
    private com.slack.api.model.view.ViewSubmit viewSubmit(String text) {
        return com.slack.api.model.view.ViewSubmit.builder()
                .type("plain_text")
                .text(text)
                .build();
    }
    
    private com.slack.api.model.view.ViewClose viewClose(String text) {
        return com.slack.api.model.view.ViewClose.builder()
                .type("plain_text")
                .text(text)
                .build();
    }
    
    private List<LayoutBlock> createSimpleModalBlocks() {
        // For simplicity, return an empty list
        // In a real implementation, you would create the blocks using the Slack Block Kit
        return Collections.emptyList();
    }

    private String createStatusSummaryText(List<DailyStatus> statuses) {
        if (statuses.isEmpty()) {
            return "No status updates available.\n";
        }

        StringBuilder summary = new StringBuilder();
        
        for (DailyStatus status : statuses) {
            summary.append("*<@").append(status.getDeveloperId()).append(">*\n");
            summary.append("*Availability:* ").append(status.getAvailability()).append("\n");
            summary.append("*Tasks:* ").append(status.getTasks()).append("\n");
            
            if (status.getNotes() != null && !status.getNotes().isEmpty()) {
                summary.append("*Notes:* ").append(status.getNotes()).append("\n");
            }
            
            summary.append("\n");
        }
        
        return summary.toString();
    }

    public SlackAppServlet getServlet() {
        return new SlackAppServlet(app);
    }
} 