package com.victor.industrial_api.service;

import com.victor.industrial_api.entity.HistoricalPointEntity;
import com.victor.industrial_api.model.Quality;
import com.victor.industrial_api.repository.HistoricalPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataSeedService {
    private final HistoricalPointRepository repository;
    private final TagHistoryService tagHistoryService;

    public DataSeedService(HistoricalPointRepository repository, TagHistoryService tagHistoryService) {
        this.repository = repository;
        this.tagHistoryService = tagHistoryService;
    }

    @Transactional
    public int seedHistoricalData(String tagName, int days, int pointsPerDay) {
        // Validation of parameters
        TagHistoryService.TagConfig config = tagHistoryService.getTagConfig(tagName);

        if (days < 1) {
            throw new IllegalArgumentException("days has to be at least 1");
        }

        if (pointsPerDay < 1) {
            throw new IllegalArgumentException("pointsPerDay has to be at least 1");
        }

        List<HistoricalPointEntity> points = new ArrayList<>();

        // Time range
        Instant endTime = Instant.now();
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);

        // Interval between points = timeSeconds / pointsPerDay
        long totalSeconds = ChronoUnit.SECONDS.between(startTime, endTime);
        int totalPoints = days * pointsPerDay;
        long intervalSeconds = totalSeconds / totalPoints;

        // Generate points with realistic variation
        Instant currentTime = startTime;
        float baseValue = config.baseValue();
        float currentValue = baseValue;

        for (int i = 0; i < totalPoints; i++) {
            // Simulate realistic drift
            float drift = (float) (Math.random() - 0.5) * (config.range() / 10);
            currentValue += drift;

            // Keep it in bounds
            float minValue = baseValue - config.range() / 2;
            float maxValue = baseValue + config.range() / 2;
            currentValue = Math.clamp(currentValue, minValue, maxValue);

            // Generate quality
            Quality quality = generateQuality();

            HistoricalPointEntity point = new HistoricalPointEntity(
                tagName,
                currentTime,
                currentValue,
                quality
            );

            points.add(point);
            currentTime = currentTime.plusSeconds(intervalSeconds);

        }
        // Batch insert
        repository.saveAll(points);

        return points.size();
    }

    private Quality generateQuality() {
        int random = (int) (Math.random() * 100);
        if (random < 1) return Quality.BAD;
        if (random < 5) return Quality.UNCERTAIN;
        return Quality.GOOD;
    }

    @Transactional
    public int clearData(String tagName) {
        if (tagName != null) {
            List<HistoricalPointEntity> points = repository.findByTagName(tagName);
            repository.deleteAll(points);
            return points.size();
        } else {
            long count = repository.count();
            repository.deleteAll();
            return (int) count;
        }
    }
}
