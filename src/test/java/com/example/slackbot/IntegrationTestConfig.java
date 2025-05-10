package com.example.slackbot;

import com.example.slackbot.adapters.secondary.OutlookCalendarAdapter;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("integrationTest")
public class IntegrationTestConfig {
    
    // Provide a mock for OutlookCalendarAdapter to avoid environment variable dependencies
    @Bean
    @Primary
    public OutlookCalendarAdapter mockOutlookCalendarAdapter() {
        return Mockito.mock(OutlookCalendarAdapter.class);
    }
} 