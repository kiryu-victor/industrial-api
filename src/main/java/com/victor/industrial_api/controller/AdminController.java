package com.victor.industrial_api.controller;

import com.victor.industrial_api.service.DataSeedService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DataSeedService dss;

    public AdminController(DataSeedService dataSeedService) {
        this.dss = dataSeedService;
    }

    // --- CREATE ---
    // One specific
    @PostMapping("/seed")
    public String seedData(
            @RequestParam String tag,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "100") int pointsPerDay) {
        int points = dss.seedHistoricalData(tag, days, pointsPerDay);

        return String.format("Seeded %d points for tag '%s' over %d days", points, tag, days);
    }

    // DELETE
    @DeleteMapping("/clear")
    public String deleteData(@RequestParam(required = false) String tag) {
        int deleted = dss.clearData(tag);

        if (tag != null) {
            return String.format("Deleted %d points for tag '%s'", deleted, tag);
        } else {
            return String.format("Deleted %d points", deleted);
        }
    }
}
