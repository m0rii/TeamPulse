package com.example.slackbot;

import com.example.slackbot.adapters.secondary.OutlookCalendarAdapter;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfig {
    
    // Provide a mock for OutlookCalendarAdapter to avoid environment variable dependencies
    @Bean
    @Primary
    public OutlookCalendarAdapter mockOutlookCalendarAdapter() {
        return Mockito.mock(OutlookCalendarAdapter.class);
    }
} 