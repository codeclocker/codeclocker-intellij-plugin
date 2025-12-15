package com.codeclocker.plugin.intellij.analytics;

import java.util.Map;

/**
 * Represents a single analytics event.
 *
 * @param eventType The type of event (use constants from {@link AnalyticsEventType})
 * @param timestamp Unix timestamp in milliseconds when the event occurred
 * @param properties Additional event-specific properties
 */
public record AnalyticsEvent(String eventType, long timestamp, Map<String, Object> properties) {

  public static AnalyticsEvent of(String eventType) {
    return new AnalyticsEvent(eventType, System.currentTimeMillis(), Map.of());
  }

  public static AnalyticsEvent of(String eventType, Map<String, Object> properties) {
    return new AnalyticsEvent(eventType, System.currentTimeMillis(), properties);
  }
}
