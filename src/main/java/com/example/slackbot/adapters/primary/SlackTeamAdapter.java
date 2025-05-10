package com.example.slackbot.adapters.primary;

import com.example.slackbot.application.TeamService;
import com.example.slackbot.domain.Team;
import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.view.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SlackTeamAdapter {
    private final App app;
    private final TeamService teamService;

    @Autowired
    public SlackTeamAdapter(App app, TeamService teamService) {
        this.app = app;
        this.teamService = teamService;
    }

    @PostConstruct
    public void init() {
        // Handle team management command
        app.command("/team", (req, ctx) -> {
            String text = req.getPayload().getText();
            String userId = req.getPayload().getUserId();
            String[] args = text.split("\\s+", 2);
            
            if (args.length == 0 || args[0].isEmpty()) {
                return ctx.ack(SlashCommandResponse.builder()
                    .text("Usage: /team [create|list|join|leave|add|remove|promote|demote|info] [args]")
                    .build());
            }
            
            String subCommand = args[0].toLowerCase();
            String subArgs = args.length > 1 ? args[1] : "";
            
            switch (subCommand) {
                case "create":
                    return handleCreateTeam(subArgs, userId, ctx);
                case "list":
                    return handleListTeams(userId, ctx);
                case "join":
                    return handleJoinTeam(subArgs, userId, ctx);
                case "leave":
                    return handleLeaveTeam(subArgs, userId, ctx);
                case "add":
                    return handleAddMember(subArgs, userId, ctx);
                case "remove":
                    return handleRemoveMember(subArgs, userId, ctx);
                case "promote":
                    return handlePromoteManager(subArgs, userId, ctx);
                case "demote":
                    return handleDemoteManager(subArgs, userId, ctx);
                case "info":
                    return handleTeamInfo(subArgs, ctx);
                default:
                    return ctx.ack(SlashCommandResponse.builder()
                        .text("Unknown subcommand. Use: create, list, join, leave, add, remove, promote, demote, or info")
                        .build());
            }
        });
    }

    private Response handleCreateTeam(String args, String userId, SlashCommandContext ctx) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 1 || parts[0].isEmpty()) {
            return ctx.ack("Usage: /team create [team_name] [optional_description]");
        }
        
        String teamName = parts[0];
        String description = parts.length > 1 ? parts[1] : "";
        
        Team team = new Team();
        team.setName(teamName);
        team.setDescription(description);
        team.addManager(userId);
        
        Team createdTeam = teamService.createTeam(team);
        
        return ctx.ack("Team *" + teamName + "* created successfully with ID: " + createdTeam.getId());
    }

    private Response handleListTeams(String userId, SlashCommandContext ctx) {
        List<Team> teams = teamService.getAllTeams();
        
        if (teams.isEmpty()) {
            return ctx.ack("No teams found.");
        }
        
        StringBuilder sb = new StringBuilder("*Available Teams:*\n");
        for (Team team : teams) {
            sb.append("• *").append(team.getName()).append("* (ID: ").append(team.getId()).append(")");
            if (team.isMember(userId)) {
                sb.append(" - You are a member");
            }
            if (team.isManager(userId)) {
                sb.append(" - You are a manager");
            }
            sb.append("\n");
        }
        
        return ctx.ack(sb.toString());
    }

    private Response handleJoinTeam(String teamId, String userId, SlashCommandContext ctx) {
        if (teamId.isEmpty()) {
            return ctx.ack("Usage: /team join [team_id]");
        }
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (team.isMember(userId)) {
            return ctx.ack("You are already a member of team: " + team.getName());
        }
        
        teamService.addUserToTeam(teamId, userId);
        return ctx.ack("You have joined team: *" + team.getName() + "*");
    }

    private Response handleLeaveTeam(String teamId, String userId, SlashCommandContext ctx) {
        if (teamId.isEmpty()) {
            return ctx.ack("Usage: /team leave [team_id]");
        }
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (!team.isMember(userId)) {
            return ctx.ack("You are not a member of team: " + team.getName());
        }
        
        if (team.isManager(userId)) {
            // Check if this is the last manager
            if (team.getManagerIds().size() <= 1) {
                return ctx.ack("You are the only manager of this team. Please promote another member to manager before leaving.");
            }
            
            // Remove as manager first
            team.removeManager(userId);
            teamService.updateTeam(team);
        }
        
        teamService.removeUserFromTeam(teamId, userId);
        return ctx.ack("You have left team: *" + team.getName() + "*");
    }

    private Response handleAddMember(String args, String userId, SlashCommandContext ctx) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return ctx.ack("Usage: /team add [team_id] [user_id]");
        }
        
        String teamId = parts[0];
        String memberToAddId = parts[1];
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (!team.isManager(userId)) {
            return ctx.ack("You must be a manager of the team to add members.");
        }
        
        if (team.isMember(memberToAddId)) {
            return ctx.ack("<@" + memberToAddId + "> is already a member of team: " + team.getName());
        }
        
        teamService.addUserToTeam(teamId, memberToAddId);
        return ctx.ack("Added <@" + memberToAddId + "> to team: *" + team.getName() + "*");
    }

    private Response handleRemoveMember(String args, String userId, SlashCommandContext ctx) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return ctx.ack("Usage: /team remove [team_id] [user_id]");
        }
        
        String teamId = parts[0];
        String memberToRemoveId = parts[1];
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (!team.isManager(userId)) {
            return ctx.ack("You must be a manager of the team to remove members.");
        }
        
        if (memberToRemoveId.equals(userId)) {
            return ctx.ack("You cannot remove yourself from the team. Use /team leave instead.");
        }
        
        if (!team.isMember(memberToRemoveId)) {
            return ctx.ack("<@" + memberToRemoveId + "> is not a member of team: " + team.getName());
        }
        
        // If removing a manager, check if they're the last manager
        if (team.isManager(memberToRemoveId) && team.getManagerIds().size() <= 1) {
            return ctx.ack("<@" + memberToRemoveId + "> is the last manager of the team. Please promote another member to manager first.");
        }
        
        // If they are a manager, remove manager status first
        if (team.isManager(memberToRemoveId)) {
            team.removeManager(memberToRemoveId);
            teamService.updateTeam(team);
        }
        
        teamService.removeUserFromTeam(teamId, memberToRemoveId);
        return ctx.ack("Removed <@" + memberToRemoveId + "> from team: *" + team.getName() + "*");
    }
    
    private Response handlePromoteManager(String args, String userId, SlashCommandContext ctx) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return ctx.ack("Usage: /team promote [team_id] [user_id]");
        }
        
        String teamId = parts[0];
        String memberToPromoteId = parts[1];
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (!team.isManager(userId)) {
            return ctx.ack("You must be a manager of the team to promote members.");
        }
        
        if (!team.isMember(memberToPromoteId)) {
            return ctx.ack("<@" + memberToPromoteId + "> is not a member of team: " + team.getName());
        }
        
        if (team.isManager(memberToPromoteId)) {
            return ctx.ack("<@" + memberToPromoteId + "> is already a manager of team: " + team.getName());
        }
        
        team.addManager(memberToPromoteId);
        teamService.updateTeam(team);
        
        return ctx.ack("Promoted <@" + memberToPromoteId + "> to manager of team: *" + team.getName() + "*");
    }
    
    private Response handleDemoteManager(String args, String userId, SlashCommandContext ctx) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return ctx.ack("Usage: /team demote [team_id] [user_id]");
        }
        
        String teamId = parts[0];
        String managerToDemoteId = parts[1];
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        if (!team.isManager(userId)) {
            return ctx.ack("You must be a manager of the team to demote other managers.");
        }
        
        if (!team.isManager(managerToDemoteId)) {
            return ctx.ack("<@" + managerToDemoteId + "> is not a manager of team: " + team.getName());
        }
        
        // Check if trying to demote the last manager
        if (team.getManagerIds().size() <= 1) {
            return ctx.ack("Cannot demote the last manager of the team. Promote another member to manager first.");
        }
        
        // Prevent self-demotion if you're the only manager
        if (managerToDemoteId.equals(userId) && team.getManagerIds().size() <= 1) {
            return ctx.ack("You cannot demote yourself as the only manager. Promote another member to manager first.");
        }
        
        team.removeManager(managerToDemoteId);
        teamService.updateTeam(team);
        
        return ctx.ack("Demoted <@" + managerToDemoteId + "> from manager role in team: *" + team.getName() + "*");
    }

    private Response handleTeamInfo(String teamId, SlashCommandContext ctx) {
        if (teamId.isEmpty()) {
            return ctx.ack("Usage: /team info [team_id]");
        }
        
        Optional<Team> teamOpt = teamService.getTeamById(teamId);
        if (!teamOpt.isPresent()) {
            return ctx.ack("Team not found with ID: " + teamId);
        }
        
        Team team = teamOpt.get();
        Set<String> members = team.getMemberIds();
        Set<String> managers = team.getManagerIds();
        
        StringBuilder sb = new StringBuilder();
        sb.append("*Team:* ").append(team.getName()).append("\n");
        sb.append("*ID:* ").append(team.getId()).append("\n");
        if (team.getDescription() != null && !team.getDescription().isEmpty()) {
            sb.append("*Description:* ").append(team.getDescription()).append("\n");
        }
        
        sb.append("*Managers:* ").append(managers.size()).append("\n");
        for (String managerId : managers) {
            sb.append("• <@").append(managerId).append("> (Manager)\n");
        }
        
        sb.append("*Members:* ").append(members.size() - managers.size()).append("\n");
        for (String memberId : members) {
            if (!managers.contains(memberId)) {
                sb.append("• <@").append(memberId).append(">\n");
            }
        }
        
        return ctx.ack(sb.toString());
    }
} 