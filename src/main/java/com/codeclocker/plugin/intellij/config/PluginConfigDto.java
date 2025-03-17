package com.codeclocker.plugin.intellij.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginConfigDto(
    int activityDataFlushFrequencySeconds,
    int checkApiKeyStateFrequencySeconds,
    int nextConfigLoadAfterLastSuccessfulLoadMinutes) {}
