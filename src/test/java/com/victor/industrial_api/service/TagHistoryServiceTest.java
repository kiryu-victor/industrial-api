package com.victor.industrial_api.service;

import com.victor.industrial_api.exception.TagNotFoundException;
import com.victor.industrial_api.repository.HistoricalPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TagHistoryServiceTest {
    private HistoricalPointRepository repository;
    private TagHistoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(HistoricalPointRepository.class);
        service = new TagHistoryService(repository);
    }

    @Test
    void getTagConfig_temperature_returnCorrectConfig() {
        TagHistoryService.TagConfig config = service.getTagConfig("temperature");

        assertEquals(0.0f, config.baseValue());
        assertEquals(10.0f, config.range());
        assertEquals(10, config.intervalSeconds());
    }

    @Test
    void getTagConfig_invalidTag_throwsTagNotFoundException() {
        assertThrows(TagNotFoundException.class, () -> {
            service.getTagConfig("invalid_tag");
        });
    }

    @Test
    void getTagConfig_pressure_returnCorrectConfig() {
        TagHistoryService.TagConfig config = service.getTagConfig("pressure");

        assertEquals(20.0f, config.baseValue());
        assertEquals(20.0f, config.range());
        assertEquals(20, config.intervalSeconds());
    }

    @Test
    void getHistoricalData_nullTagName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getHistoricalData(null, 10, null, null, null);
        });
    }

    @Test
    void getHistoricalData_emptyTagName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
           service.getHistoricalData("", 10, null, null, null);
        });
    }

    @Test
    void getHistoricalData_pointsTooLow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getHistoricalData("temperature", 0, null, null, null);
        });
    }

    @Test
    void getHistoricalData_pointsTooHigh_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getHistoricalData("temperature", 1001, String.valueOf(0), null, null);
        });
    }

    @Test
    void getHistoricalData_startAfterEnd_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getHistoricalData(
                    "temperature", 1001,
                    "2026-05-21T12:00:00Z",
                    "2026-05-21T10:00:00Z",
                    null
            );
        });
    }

    @Test
    void getHistoricalData_invalidTag_throwsTagNotFoundException() {
        assertThrows(TagNotFoundException.class, () -> {
            service.getHistoricalData("invalid_tag", 10, null,null,null);
        });
    }
}
