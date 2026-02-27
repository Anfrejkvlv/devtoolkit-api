package com.devtoolkit.config;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@AllArgsConstructor
public class GeminiApiKey {
    private final String key;
    private Instant exhaustedAt;
    private int failureCount;

    public GeminiApiKey(String key) {
        this.key = key;
        this.exhaustedAt = null;
        this.failureCount = 0;
    }

    public boolean isAvailable() {
        if (exhaustedAt == null) return true;
        return Instant.now().isAfter(exhaustedAt.plus(Duration.ofMinutes(60)));
    }

    public void markExhausted() {
        this.exhaustedAt = Instant.now();
        this.failureCount++;
    }

    public void reset() {
        this.exhaustedAt = null;
        this.failureCount = 0;
    }
}
