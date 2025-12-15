package com.codeclocker.plugin.intellij.analytics;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.codeclocker.plugin.intellij.reporting.SentStatus;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduled task that flushes analytics events to the backend every 5 minutes. This is an
 * application-level service that manages analytics collection and reporting.
 */
public class AnalyticsReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(AnalyticsReportingTask.class);

  private static final int FLUSH_INTERVAL_MINUTES = 5;

  private final AnalyticsQueue analyticsQueue;
  private final AnalyticsHttpClient analyticsHttpClient;

  private ScheduledFuture<?> task;
  private IdeContext cachedIdeContext;

  public AnalyticsReportingTask() {
    this.analyticsQueue = new AnalyticsQueue();
    this.analyticsHttpClient =
        ApplicationManager.getApplication().getService(AnalyticsHttpClient.class);
  }

  /** Starts the scheduled analytics reporting task. */
  public void schedule() {
    if (task != null && !task.isCancelled()) {
      return;
    }

    // Track plugin started event
    track(AnalyticsEventType.PLUGIN_STARTED);

    task =
        EXECUTOR.scheduleWithFixedDelay(
            this::flushAnalytics, FLUSH_INTERVAL_MINUTES, FLUSH_INTERVAL_MINUTES, MINUTES);

    LOG.info("Analytics reporting task scheduled (every " + FLUSH_INTERVAL_MINUTES + " minutes)");
  }

  /**
   * Tracks an analytics event.
   *
   * @param eventType The type of event to track
   */
  public void track(String eventType) {
    analyticsQueue.track(eventType);
  }

  /**
   * Tracks an analytics event with properties.
   *
   * @param eventType The type of event to track
   * @param properties Additional event properties
   */
  public void track(String eventType, Map<String, Object> properties) {
    analyticsQueue.track(eventType, properties);
  }

  /** Flushes queued analytics events to the backend. */
  public void flushAnalytics() {
    try {
      List<AnalyticsEvent> events = analyticsQueue.drain();
      if (events.isEmpty()) {
        LOG.debug("No analytics events to flush");
        return;
      }

      LOG.debug("Flushing " + events.size() + " analytics events");

      String installationId = InstallationIdPersistence.getInstallationId();
      IdeContext ideContext = getIdeContext();

      AnalyticsReportDto report = AnalyticsReportDto.from(installationId, ideContext, events);
      SentStatus status = analyticsHttpClient.sendAnalyticsReport(report);

      if (status == ERROR) {
        LOG.warn("Failed to send analytics report, re-queuing " + events.size() + " events");
        // Re-queue events for retry
        for (AnalyticsEvent event : events) {
          analyticsQueue.track(event.eventType(), event.properties());
        }
      } else {
        LOG.info("Successfully sent " + events.size() + " analytics events");
      }
    } catch (Exception ex) {
      LOG.warn("Error flushing analytics: " + ex.getMessage(), ex);
    }
  }

  private IdeContext getIdeContext() {
    if (cachedIdeContext == null) {
      cachedIdeContext = IdeContext.current();
    }
    return cachedIdeContext;
  }

  @Override
  public void dispose() {
    LOG.info("Disposing AnalyticsReportingTask - flushing remaining events");

    // Track plugin stopped event
    track(AnalyticsEventType.PLUGIN_STOPPED);

    try {
      // Perform final flush
      flushAnalytics();
      LOG.info("Final analytics flush completed");
    } catch (Exception e) {
      LOG.warn("Error during final analytics flush", e);
    }

    if (task != null) {
      task.cancel(false);
    }

    LOG.info("AnalyticsReportingTask disposed");
  }
}
