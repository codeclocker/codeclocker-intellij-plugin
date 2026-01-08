package com.codeclocker.plugin.intellij.reporting;

/**
 * DTO for time spent sample sent to the hub.
 *
 * @param recordId unique identifier for this record (for idempotent sync), nullable for backward
 *     compatibility
 * @param hourKey hour bucket in format "yyyy-MM-dd-HH" in UTC timezone (e.g., "2025-12-28-10")
 * @param deltaSeconds seconds accumulated since last report (increment)
 * @param totalHourSeconds total seconds for this hour bucket (for verification)
 */
public record TimeSpentSampleDto(
    String recordId, String hourKey, long deltaSeconds, long totalHourSeconds) {}
