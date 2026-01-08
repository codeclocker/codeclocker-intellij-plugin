package com.codeclocker.plugin.intellij.services;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jetbrains.annotations.Nullable;

/**
 * Accumulates coding time for a single project within an hour bucket. Thread-safe via synchronized
 * methods and volatile fields. Hour keys are stored in UTC timezone.
 */
public class ProjectTimeAccumulator {

  public static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
  private static final ZoneId UTC = ZoneId.of("UTC");

  private static final long MILLIS_PER_SECOND = 1000L;

  private volatile String hourKey;
  private volatile long accumulatedSeconds;
  private volatile long lastActivityTimestampMillis;
  private volatile boolean active;
  private volatile long lastReportedSeconds;

  public ProjectTimeAccumulator() {
    this.hourKey = ZonedDateTime.now(UTC).format(HOUR_KEY_FORMATTER);
    this.accumulatedSeconds = 0;
    this.lastActivityTimestampMillis = 0;
    this.active = false;
    this.lastReportedSeconds = 0;
  }

  public synchronized void activate(long timestampMillis) {
    this.active = true;
    this.lastActivityTimestampMillis = timestampMillis;
  }

  public synchronized void deactivate() {
    this.active = false;
  }

  public synchronized void calculateAndAddElapsed(long now) {
    if (!active || lastActivityTimestampMillis == 0) {
      return;
    }

    long elapsedMillis = now - lastActivityTimestampMillis;
    long elapsedSeconds = Math.round((float) elapsedMillis / MILLIS_PER_SECOND);
    if (elapsedSeconds > 0) {
      this.accumulatedSeconds += elapsedSeconds;
    }
  }

  /**
   * Get the delta (unreported seconds) and mark them as reported.
   *
   * @return seconds accumulated since last report
   */
  public synchronized long getUnreportedDeltaAndMarkReported() {
    long delta = accumulatedSeconds - lastReportedSeconds;
    this.lastReportedSeconds = accumulatedSeconds;

    return delta;
  }

  /**
   * Check if hour has changed and finalize old hour data if so.
   *
   * @return HourTransition with old hour data if hour changed, null otherwise
   */
  @Nullable
  public synchronized HourTransition checkAndHandleHourBoundary() {
    String currentHour = ZonedDateTime.now(UTC).format(HOUR_KEY_FORMATTER);
    if (!currentHour.equals(hourKey)) {
      HourTransition transition =
          new HourTransition(hourKey, accumulatedSeconds, lastReportedSeconds);

      // Reset for new hour
      hourKey = currentHour;
      accumulatedSeconds = 0;
      lastReportedSeconds = 0;
      lastActivityTimestampMillis = 0;

      return transition;
    }

    return null;
  }

  public String getHourKey() {
    return hourKey;
  }

  public long getAccumulatedSeconds() {
    return accumulatedSeconds;
  }

  /**
   * Get the current unsaved delta (time accumulated since last report/flush). Does NOT mark as
   * reported - use this for display purposes only.
   *
   * @return seconds accumulated since last report
   */
  public long getUnsavedDelta() {
    return accumulatedSeconds - lastReportedSeconds;
  }

  public synchronized void setAccumulatedSeconds(long seconds) {
    this.accumulatedSeconds = seconds;
  }

  /** Result of hour boundary check containing data for the finalized hour. */
  public record HourTransition(String hourKey, long accumulatedSeconds, long lastReportedSeconds) {

    public long getDelta() {
      return accumulatedSeconds - lastReportedSeconds;
    }

    public boolean hasUnreportedSeconds() {
      return getDelta() > 0;
    }
  }
}
