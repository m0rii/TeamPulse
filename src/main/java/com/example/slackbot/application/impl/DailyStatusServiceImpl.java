package com.example.slackbot.application.impl;

import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
import com.example.slackbot.application.DailyStatusService;
import com.example.slackbot.domain.DailyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailyStatusServiceImpl implements DailyStatusService {
    private final CloudflareKVAdapter cloudflareKVAdapter;

    @Autowired
    public DailyStatusServiceImpl(CloudflareKVAdapter cloudflareKVAdapter) {
        this.cloudflareKVAdapter = cloudflareKVAdapter;
    }

    @Override
    public void addDailyStatus(DailyStatus status) {
        cloudflareKVAdapter.storeDailyStatus(status);
    }

    @Override
    public List<DailyStatus> getDailyStatuses(String date) {
        return cloudflareKVAdapter.retrieveDailyStatuses(date);
    }
} 