package com.victor.industrial_api.model;

import java.time.Instant;

public record HistoricalPoint (Instant timestamp, float value, Quality quality) {}