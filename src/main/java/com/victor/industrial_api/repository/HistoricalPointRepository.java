package com.victor.industrial_api.repository;

import com.victor.industrial_api.entity.HistoricalPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HistoricalPointRepository extends JpaRepository<HistoricalPointEntity, Long> {
    // Find all points for a specific tag
    List<HistoricalPointEntity> findByTagName(String tagName);

    // Find points for a tag within a time range
    List<HistoricalPointEntity> findByTagNameAndTimeStampBetween(
            String tagName,
            Instant start,
            Instant end
    );

    // Find the latest point for a tag
    HistoricalPointEntity findFirstByTagNameOrderByTimeStampDesc(String tagName);
}