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
    
    @Value("${scheduling.timezone}")
    private String timezone;
    
    @Value("${scheduling.dailyReminderTime}")
    private String reminderTime;

    @Autowired
    public SchedulingConfig(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "CET")
    public void sendDailyReminder() {
        reminderService.sendDailyReminders();
    }
} 