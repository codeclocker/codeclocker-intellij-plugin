package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.intellij.openapi.Disposable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TimeSpentActivityTracker implements Disposable {

  private final TimeSpentPerProjectLogger timeSpentPerProjectLogger =
      new TimeSpentPerProjectLogger();
  private final long pauseActivityAfterInactivityMillis = Duration.ofSeconds(30).toMillis();
  private final AtomicReference<ScheduledFuture<?>> scheduledTask;
  private final AtomicLong lastRescheduledAt = new AtomicLong();

  public TimeSpentActivityTracker() {
    this.scheduledTask = new AtomicReference<>(schedule());
  }

  public Map<String, TimeSpentPerProjectSample> drain() {
    return timeSpentPerProjectLogger.drain();
  }

  public void logTime(String project) {
    rescheduleInactivityTask();
    timeSpentPerProjectLogger.log(project);
  }

  public void rescheduleInactivityTask() {
    long now = System.currentTimeMillis();
    if (now - lastRescheduledAt.get() < 1000) {
      return;
    }

    lastRescheduledAt.set(now);

    scheduledTask.updateAndGet(
        currentTask -> {
          currentTask.cancel(false);
          return schedule();
        });
  }

  private ScheduledFuture<?> schedule() {
    return EXECUTOR.schedule(this::pause, pauseActivityAfterInactivityMillis, MILLISECONDS);
  }

  public void pause() {
    timeSpentPerProjectLogger.pauseDueToInactivity();
  }

  @Override
  public void dispose() {
    scheduledTask.get().cancel(false);
    EXECUTOR.shutdown();
  }
}
