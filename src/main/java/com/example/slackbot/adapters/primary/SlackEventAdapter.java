package com.example.slackbot.adapters.primary;

import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.domain.DailyStatus;
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

@Component
public class SlackEventAdapter {
    private final App app;
    private final DailyStatusService dailyStatusService;

    @Autowired
    public SlackEventAdapter(App app, DailyStatusService dailyStatusService) {
        this.app = app;
        this.dailyStatusService = dailyStatusService;
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
            
            return ctx.ack();
        });

        // Handle slash command
        app.command("/status", (req, ctx) -> {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            List<DailyStatus> statuses = dailyStatusService.getDailyStatuses(date);
            
            return ctx.ack(r -> r.text(createStatusSummaryText(statuses)));
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
        StringBuilder summary = new StringBuilder("*Today's Team Status*\n\n");
        
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