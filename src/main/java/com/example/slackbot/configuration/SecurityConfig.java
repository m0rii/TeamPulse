package com.example.slackbot.configuration;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Value("${slack.signingSecret}")
    private String signingSecret;

    @Value("${slack.botToken}")
    private String botToken;

    @Bean
    public App slackApp() {
        AppConfig config = AppConfig.builder()
            .signingSecret(signingSecret)
            .singleTeamBotToken(botToken)
            .build();
        return new App(config);
    }
} 