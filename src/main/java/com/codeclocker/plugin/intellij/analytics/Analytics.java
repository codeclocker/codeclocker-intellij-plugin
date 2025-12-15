package com.codeclocker.plugin.intellij.analytics;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Simple facade for tracking analytics events from anywhere in the plugin. Usage:
 *
 * <pre>
 *   Analytics.track(AnalyticsEventType.WIDGET_CLICK);
 *   Analytics.track(AnalyticsEventType.WIDGET_POPUP_ACTION, Map.of("action", "pause"));
 * </pre>
 */
public final class Analytics {

  private static final Logger LOG = Logger.getInstance(Analytics.class);

  private Analytics() {}

  /**
   * Tracks an analytics event.
   *
   * @param eventType The type of event to track (use constants from {@link AnalyticsEventType})
   */
  public static void track(@NotNull String eventType) {
    track(eventType, Map.of());
  }

  /**
   * Tracks an analytics event with properties.
   *
   * @param eventType The type of event to track
   * @param properties Additional event-specific properties
   */
  public static void track(@NotNull String eventType, @NotNull Map<String, Object> properties) {
    try {
      AnalyticsReportingTask task =
          ApplicationManager.getApplication().getService(AnalyticsReportingTask.class);
      if (task != null) {
        task.track(eventType, properties);
      } else {
        LOG.debug("AnalyticsReportingTask not available, event not tracked: " + eventType);
      }
    } catch (Exception ex) {
      LOG.debug("Failed to track event: " + eventType, ex);
    }
  }
}
