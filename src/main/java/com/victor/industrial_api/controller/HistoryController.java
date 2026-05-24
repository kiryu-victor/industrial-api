package com.victor.industrial_api.controller;

import com.victor.industrial_api.dto.DataPointRequest;
import com.victor.industrial_api.entity.HistoricalPointEntity;
import com.victor.industrial_api.model.HistoricalPoint;
import com.victor.industrial_api.model.Quality;
import com.victor.industrial_api.repository.HistoricalPointRepository;
import com.victor.industrial_api.service.TagHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class HistoryController {
    @Autowired TagHistoryService ths;

    // --- READ ---
    @GetMapping("/api/tags/history")
    public List<HistoricalPoint> history(
            @RequestParam String tag,
            @RequestParam(defaultValue = "10") int points,
            @RequestParam(defaultValue = "0") int interval,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Quality quality
            ) {
        return ths.getHistoricalData(tag, points, interval, start, end, quality);
    }

    @GetMapping("/api/tags/latest")
    // Se pide el tag como parámetro en el endpoint
    // Hay que meterlo como "/api/tags/latest?tag=temperature"
    public HistoricalPoint latest(@RequestParam String tag) {
        return ths.getLatestData(tag);
    }


    @Autowired
    private HistoricalPointRepository repository;

    @GetMapping("/api/test/insert")
    public String testInsert() {
        HistoricalPointEntity point = new HistoricalPointEntity(
                "temperature",
                Instant.now(),
                25.5f,
                Quality.GOOD
        );

        repository.save(point);

        return "Point inserted with ID: " + point.getId();
    }

    @GetMapping("/api/test/list")
    public List<HistoricalPointEntity> testList() {
        return repository.findAll();
    }


    // --- CREATE ---
    @PostMapping("/api/tags/data")
    public HistoricalPoint writeData(@RequestBody DataPointRequest request) {
        return ths.writeDataPoint(
                request.tag(),
                request.value(),
                request.quality()
        );
    }

    @PostMapping("/api/tags/batch")
    public List<HistoricalPoint> writeBatch(@RequestBody List<DataPointRequest> requests) {
        return ths.writeBatch(requests);
    }
}