package com.example.slackbot.application.impl;

import com.example.slackbot.domain.DailyStatus;
import com.example.slackbot.adapters.secondary.CloudflareKVAdapter;
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

    @BeforeEach
    void setUp() {
        cloudflareKVAdapter = Mockito.mock(CloudflareKVAdapter.class);
        service = new DailyStatusServiceImpl(cloudflareKVAdapter);
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