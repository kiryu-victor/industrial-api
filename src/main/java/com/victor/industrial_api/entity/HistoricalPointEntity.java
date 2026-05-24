package com.victor.industrial_api.entity;

import com.victor.industrial_api.model.Quality;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "historical_points")
public class HistoricalPointEntity {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String tagName;

    @Column(nullable = false)
    private Instant timeStamp;

    @Column(nullable = false)
    private float value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Quality quality;

    // --- CONSTRUCTOR ---
    public HistoricalPointEntity() {}

    public HistoricalPointEntity(String tagName, Instant timeStamp, float value, Quality quality) {
        this.tagName = tagName;
        this.timeStamp = timeStamp;
        this.value = value;
        this.quality = quality;
    }

    // --- GETTERS & SETTERS ---
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(Instant timeStamp) {
        this.timeStamp = timeStamp;
    }

    public float getValue() {
        return value;
    }
    public void setValue(float value) {
        this.value = value;
    }

    public Quality getQuality() {
        return quality;
    }
    public void setQuality(Quality quality) {
        this.quality = quality;
    }
}
