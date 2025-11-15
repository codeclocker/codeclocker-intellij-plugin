package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TimeSpentActivityTracker implements Disposable {

  private final TimeSpentPerProjectLogger timeSpentPerProjectLogger;
  private final long pauseActivityAfterInactivityMillis = Duration.ofMinutes(2).toMillis();
  private final AtomicReference<ScheduledFuture<?>> scheduledTask;
  private final AtomicLong lastRescheduledAt = new AtomicLong();

  public TimeSpentActivityTracker() {
    this.scheduledTask = new AtomicReference<>(schedule());
    this.timeSpentPerProjectLogger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
  }

  public void logTime(Project project) {
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
    ScheduledFuture<?> task = scheduledTask.get();
    if (task != null) {
      task.cancel(false);
    }
  }
}
