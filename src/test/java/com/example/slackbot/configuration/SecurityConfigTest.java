package com.example.slackbot.configuration;

import com.slack.api.bolt.App;
import com.example.slackbot.configuration.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityConfigTest {
    private SecurityConfig securityConfig;
    private App app;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Test
    void testSlackAppInitialization() {
        app = securityConfig.slackApp();
        assertNotNull(app);
    }
} 