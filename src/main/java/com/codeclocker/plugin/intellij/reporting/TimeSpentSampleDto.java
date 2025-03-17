package com.codeclocker.plugin.intellij.reporting;

import java.util.Map;

public record TimeSpentSampleDto(
    long samplingStartedAt,
    long timeSpentSeconds,
    long additions,
    long removals,
    Map<String, String> metadata) {}
