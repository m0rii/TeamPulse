package com.example.slackbot.configuration;

import com.example.slackbot.application.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZoneId;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final ReminderService reminderService;
    private final String timezone;
    
    @Autowired
    public SchedulingConfig(
            ReminderService reminderService,
            @Value("${scheduling.timezone}") String timezone) {
        this.reminderService = reminderService;
        this.timezone = timezone;
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "${scheduling.timezone}")
    public void sendDailyReminder() {
        reminderService.sendDailyReminders();
    }
} 