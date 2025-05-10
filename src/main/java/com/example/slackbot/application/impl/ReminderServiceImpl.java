package com.example.slackbot.application.impl;

import com.example.slackbot.application.ReminderService;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    private final App app;
    private final List<String> developerIds = Arrays.asList("U01", "U02", "U03", "U04", "U05", "U06", "U07", "U08");

    @Autowired
    public ReminderServiceImpl(App app) {
        this.app = app;
    }

    @Override
    public void sendDailyReminders() {
        for (String developerId : developerIds) {
            try {
                sendReminderToUser(developerId);
            } catch (Exception e) {
                // Log the error and continue with the next user
                System.err.println("Failed to send reminder to user " + developerId + ": " + e.getMessage());
            }
        }
    }

    private void sendReminderToUser(String userId) throws IOException, SlackApiException {
        List<LayoutBlock> blocks = new ArrayList<>();
        
        blocks.add(SectionBlock.builder()
                .text(PlainTextObject.builder()
                        .text("Good morning! Please set your daily status.")
                        .build())
                .build());
        
        blocks.add(ActionsBlock.builder()
                .elements(Arrays.asList(
                        ButtonElement.builder()
                                .text(PlainTextObject.builder()
                                        .text("Set Status")
                                        .build())
                                .actionId("set_status")
                                .build()))
                .build());
        
        ChatPostMessageResponse response = app.client().chatPostMessage(r -> r
                .channel(userId)
                .blocks(blocks)
        );
        
        if (!response.isOk()) {
            throw new RuntimeException("Failed to send reminder: " + response.getError());
        }
    }
} 