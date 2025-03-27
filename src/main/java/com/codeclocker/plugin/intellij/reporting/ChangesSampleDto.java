package com.codeclocker.plugin.intellij.reporting;

import java.util.Map;

public record ChangesSampleDto(
    long samplingStartedAt, long additions, long removals, Map<String, String> metadata) {}
