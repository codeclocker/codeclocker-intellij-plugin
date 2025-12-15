package com.codeclocker.plugin.intellij.analytics;

import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe queue for collecting analytics events. Events are accumulated and can be drained for
 * batch reporting.
 */
public class AnalyticsQueue {

  private static final Logger LOG = Logger.getInstance(AnalyticsQueue.class);

  private static final int MAX_QUEUE_SIZE = 1000;

  private final ConcurrentLinkedQueue<AnalyticsEvent> events = new ConcurrentLinkedQueue<>();

  /**
   * Tracks an analytics event.
   *
   * @param eventType The type of event to track
   */
  public void track(String eventType) {
    track(eventType, Map.of());
  }

  /**
   * Tracks an analytics event with properties.
   *
   * @param eventType The type of event to track
   * @param properties Additional event properties
   */
  public void track(String eventType, Map<String, Object> properties) {
    if (events.size() >= MAX_QUEUE_SIZE) {
      LOG.warn("Analytics queue is full, dropping oldest event");
      events.poll();
    }

    AnalyticsEvent event = AnalyticsEvent.of(eventType, properties);
    events.add(event);
    LOG.debug("Tracked analytics event: " + eventType);
  }

  /**
   * Drains all events from the queue.
   *
   * @return List of all queued events (queue is emptied)
   */
  public List<AnalyticsEvent> drain() {
    List<AnalyticsEvent> drained = new ArrayList<>();
    AnalyticsEvent event;
    while ((event = events.poll()) != null) {
      drained.add(event);
    }
    return drained;
  }

  /** Returns the number of events in the queue. */
  public int size() {
    return events.size();
  }

  /** Checks if the queue is empty. */
  public boolean isEmpty() {
    return events.isEmpty();
  }
}
