package com.victor.industrial_api.service;

import com.victor.industrial_api.dto.DataPointRequest;
import com.victor.industrial_api.entity.HistoricalPointEntity;
import com.victor.industrial_api.exception.TagNotFoundException;
import com.victor.industrial_api.model.HistoricalPoint;
import com.victor.industrial_api.model.Quality;
import com.victor.industrial_api.repository.HistoricalPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// --- BUSINESS LOGIC: processing data ---
// Single Responsibility Principle: takes the business logic away from CONTROLLERS
// Reusable: can be called from controllers, scheduled jobs...
// Testable: can test business logic without HTTP
@Service("ths")
public class TagHistoryService {
    // State-less: no variable on the service
    // The lists are generated on demand
    // One place to handle all the possible cases for the query parameters



    // ----- TAG CONFIG -----
    public record TagConfig(float baseValue, float range, int intervalSeconds) {};

    public TagConfig getTagConfig(String tagName) {
        return switch (tagName) {
            case "flow" -> new TagConfig(100.0f, 50.0f, 10);
            case "humidity" -> new TagConfig(40.0f, 30.0f, 90);
            case "pressure" -> new TagConfig(20.0f, 20.0f, 20);
            case "temperature" -> new TagConfig(0.0f, 10.0f, 10);
            case "voltage" -> new TagConfig(220.0f, 20.0f, 45);
            default -> throw new TagNotFoundException(tagName);
        };
    }



    // --- REPOSITORIES ---
    private final HistoricalPointRepository repository;

    // Constructor injection
    public TagHistoryService(HistoricalPointRepository repository) {
        this.repository = repository;
    }

    // Conversion methods
    private HistoricalPoint toRecord(HistoricalPointEntity entity) {
        return new HistoricalPoint(
                entity.getTimeStamp(),
                entity.getValue(),
                entity.getQuality()
        );
    }

    private List<HistoricalPoint> toRecordList(List<HistoricalPointEntity> entities) {
        return entities.stream().map(this::toRecord).toList();
    }



    // Other methods
    // READ
    public HistoricalPoint getLatestData(String tagName) {
        getTagConfig(tagName);

        HistoricalPointEntity entity = repository.findFirstByTagNameOrderByTimeStampDesc(tagName);

        if (entity == null) {
            throw new TagNotFoundException(tagName + " (no data available)");
        }

        return toRecord(entity);
    }

    public List<HistoricalPoint> getHistoricalData(
            String tagName, int points, int customInterval,
            String start, String end, Quality quality) {
        // Validation
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        if (points < 1 || points > 1000) {
            throw new IllegalArgumentException("Points must be between 1 and 1000");
        }

        // Validate tag exists
        getTagConfig(tagName);

        // Parse timestamps
        Instant startTime = (start != null)
                ? Instant.parse(start)
                : Instant.now().minus(2, ChronoUnit.HOURS);
        Instant endTime = (end != null)
                ? Instant.parse(end)
                : Instant.now();

        // Validate time
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Starting time cannot be after end time");
        }

        // Query DB
        List<HistoricalPointEntity> entities = repository.findByTagNameAndTimeStampBetween(tagName, startTime, endTime);

        // Filter by quality
        if (quality != null) {
            entities = entities.stream()
                    .filter(e -> e.getQuality() == quality)
                    .toList();
        }

        // Limit to request points
        if (entities.size() > points) {
            entities = entities.subList(0, points);
        }

        return toRecordList(entities);
    }

    // CREATE
    @Transactional
    public HistoricalPoint writeDataPoint(String tagName, float value, Quality quality) {
        // Validate tag exists
        getTagConfig(tagName);

        // Create entity
        HistoricalPointEntity entity = new HistoricalPointEntity(
                tagName,
                Instant.now(),
                value,
                quality
        );

        // Save to database
        HistoricalPointEntity saved = repository.save(entity);

        // Return as API response format
        return toRecord(saved);
    }

    public List<HistoricalPoint> writeBatch(List<DataPointRequest> requests) {
        List<HistoricalPointEntity> entities = new ArrayList<>();
        Instant timestamp = Instant.now();

        for (DataPointRequest request: requests) {
            getTagConfig(request.tag());

            HistoricalPointEntity entity = new HistoricalPointEntity(
                    request.tag(),
                    timestamp,
                    request.value(),
                    request.quality()
            );

            entities.add(entity);
        }

        List<HistoricalPointEntity> saved = repository.saveAll(entities);

        return toRecordList(saved);
    }
}