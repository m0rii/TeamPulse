package com.example.slackbot.application;

import com.example.slackbot.domain.DailyStatus;
import java.util.List;

public interface DailyStatusService {
    void addDailyStatus(DailyStatus status);
    List<DailyStatus> getDailyStatuses(String date);
} 