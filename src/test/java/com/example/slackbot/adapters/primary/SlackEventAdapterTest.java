package com.example.slackbot.adapters.primary;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.adapters.primary.SlackEventAdapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SlackEventAdapterTest {
    private SlackEventAdapter adapter;
    private App app;
    private DailyStatusService dailyStatusService;

    @BeforeEach
    void setUp() {
        app = Mockito.mock(App.class);
        dailyStatusService = Mockito.mock(DailyStatusService.class);
        adapter = new SlackEventAdapter(app, dailyStatusService);
    }

    @Test
    void testSlackAppInitialization() {
        assertNotNull(adapter);
        assertNotNull(app);
        assertNotNull(dailyStatusService);
    }
} 