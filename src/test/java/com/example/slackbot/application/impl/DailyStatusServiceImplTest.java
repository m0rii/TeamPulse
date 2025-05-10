package com.example.slackbot.application.impl;

import com.example.slackbot.domain.DailyStatus;
import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
import com.example.slackbot.application.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collections;
import com.example.slackbot.application.impl.DailyStatusServiceImpl;

class DailyStatusServiceImplTest {
    private DailyStatusServiceImpl service;
    private CloudflareKVAdapter cloudflareKVAdapter;
    private TeamService teamService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cloudflareKVAdapter = Mockito.mock(CloudflareKVAdapter.class);
        teamService = Mockito.mock(TeamService.class);
        objectMapper = Mockito.mock(ObjectMapper.class);
        service = new DailyStatusServiceImpl(cloudflareKVAdapter, teamService, objectMapper);
    }

    @Test
    void testAddDailyStatus() {
        DailyStatus status = new DailyStatus();
        status.setDeveloperId("dev1");
        status.setDate("2023-10-10");

        service.addDailyStatus(status);
        verify(cloudflareKVAdapter).storeDailyStatus(status);
    }

    @Test
    void testGetDailyStatuses() {
        String date = "2023-10-10";
        DailyStatus mockStatus = new DailyStatus();
        mockStatus.setDeveloperId("dev1");
        mockStatus.setDate(date);
        List<DailyStatus> mockStatuses = Collections.singletonList(mockStatus);

        when(cloudflareKVAdapter.retrieveDailyStatuses(date)).thenReturn(mockStatuses);

        List<DailyStatus> statuses = service.getDailyStatuses(date);
        assertEquals(1, statuses.size());
        assertEquals("dev1", statuses.get(0).getDeveloperId());
        verify(cloudflareKVAdapter).retrieveDailyStatuses(date);
    }
} 