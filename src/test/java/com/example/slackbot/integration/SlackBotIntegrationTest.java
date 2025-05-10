package com.example.slackbot.integration;

import com.example.slackbot.SlackbotApplication;
import com.example.slackbot.adapters.primary.SlackEventAdapter;
import com.example.slackbot.application.DailyStatusService;
import com.slack.api.bolt.App;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.request.builtin.EventRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.io.IOException;
import com.slack.api.methods.SlackApiException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SlackbotApplication.class)
public class SlackBotIntegrationTest {

    @Autowired
    private SlackEventAdapter slackEventAdapter;

    @Autowired
    private DailyStatusService dailyStatusService;

    @MockBean
    private App app;

    @Test
    public void testAppMentionEventHandling() throws IOException, SlackApiException {
        // Create a mock AppMentionEvent
        AppMentionEvent event = new AppMentionEvent();
        event.setText("@bot hello");

        // Create a mock EventContext
        EventContext ctx = Mockito.mock(EventContext.class);
        when(ctx.say("Hello, I am your Slack bot!")).thenReturn(null);

        // Simulate the event handling
        doAnswer(invocation -> {
            AppMentionEvent payload = invocation.getArgument(0);
            payload.setText(event.getText());
            return null;
        }).when(app).event(Mockito.eq(AppMentionEvent.class), Mockito.any());

        // Verify the interaction
        verify(app).event(Mockito.eq(AppMentionEvent.class), Mockito.any());
    }
} 