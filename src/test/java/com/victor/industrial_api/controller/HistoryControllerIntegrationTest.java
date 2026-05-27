package com.victor.industrial_api.controller;

import com.victor.industrial_api.entity.HistoricalPointEntity;
import com.victor.industrial_api.model.Quality;
import com.victor.industrial_api.repository.HistoricalPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricalPointRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void getHistory_validTag_returns200() throws Exception {
        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "temperature"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void getHistory_invalidTag_returns404() throws Exception {
        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "invalid_tag"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Tag not found: invalid_tag"));
    }

    @Test
    void getHistory_pointsTooHigh_returns400() throws Exception {
        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "temperature")
                        .param("points", "9999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_startAfterEnd_returns400() throws Exception {
        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "temperature")
                        .param("start", "2026-05-21T12:00:00Z")
                        .param("end",   "2026-05-21T10:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_withSeededData_returnsCorrectCount() throws Exception {
        insertPoints("temperature", 5);

        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "temperature")
                        .param("points", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void getHistory_qualityFilter_returnsOnlyMatchingPoints() throws Exception {
        insertPointWithQuality("temperature", Quality.GOOD);
        insertPointWithQuality("temperature", Quality.GOOD);
        insertPointWithQuality("temperature", Quality.BAD);

        mockMvc.perform(get("/api/tags/history")
                        .param("tag", "temperature")
                        .param("quality", "GOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getLatest_withData_returnsOnePoint() throws Exception {
        insertPoints("pressure", 3);

        mockMvc.perform(get("/api/tags/latest")
                        .param("tag", "pressure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").exists())
                .andExpect(jsonPath("$.quality").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getLatest_noData_returns404() throws Exception {
        mockMvc.perform(get("/api/tags/latest")
                        .param("tag", "temperature"))
                .andExpect(status().isNotFound());
    }

    @Test
    void writeData_validRequest_returns200AndPersists() throws Exception {
        mockMvc.perform(post("/api/tags/data")
                        .contentType("application/json")
                        .content("{\"tag\":\"temperature\",\"value\":25.5,\"quality\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(25.5))
                .andExpect(jsonPath("$.quality").value("GOOD"));

        // Verify it actually persisted
        long count = repository.count();
        assertEquals(1, count);
    }

    @Test
    void writeBatch_withInvalidTag_returns404AndRollsBack() throws Exception {
        mockMvc.perform(post("/api/tags/batch")
                        .contentType("application/json")
                        .content("""
                    [
                      {"tag":"temperature","value":23.5,"quality":"GOOD"},
                      {"tag":"invalid_tag","value":100.0,"quality":"GOOD"},
                      {"tag":"pressure","value":101.3,"quality":"GOOD"}
                    ]
                """))
                .andExpect(status().isNotFound());

        // Verify rollback: nothing was saved
        assertEquals(0, repository.count());
    }

    @Test
    void writeBatch_allValidTags_persistsAllPoints() throws Exception {
        mockMvc.perform(post("/api/tags/batch")
                        .contentType("application/json")
                        .content("""
                        [
                          {"tag":"temperature","value":23.5,"quality":"GOOD"},
                          {"tag":"humidity","value":100.0,"quality":"GOOD"},
                          {"tag":"pressure","value":101.3,"quality":"GOOD"}
                        ]
                        """))
                .andExpect(status().isOk());

        assertEquals(3, repository.count());
    }

    // --- Helpers ---

    private void insertPoints(String tagName, int count) {
        for (int i = 0; i < count; i++) {
            repository.save(new HistoricalPointEntity(
                    tagName,
                    Instant.now().minusSeconds(count - i),
                    20.0f + i,
                    Quality.GOOD
            ));
        }
    }

    private void insertPointWithQuality(String tagName, Quality quality) {
        repository.save(new HistoricalPointEntity(
                tagName,
                Instant.now(),
                25.0f,
                quality
        ));
    }
}