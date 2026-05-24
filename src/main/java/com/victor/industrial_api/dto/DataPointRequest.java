package com.victor.industrial_api.dto;

import com.victor.industrial_api.model.Quality;

public record DataPointRequest(String tag, float value, Quality quality) {
}
