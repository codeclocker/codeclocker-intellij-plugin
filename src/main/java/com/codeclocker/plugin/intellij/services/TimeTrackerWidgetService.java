package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;

import com.codeclocker.plugin.intellij.stopwatch.SafeStopWatch;
import com.codeclocker.plugin.intellij.widget.TimeTrackerWidget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeTrackerWidgetService implements Disposable {

  private static final Logger LOG = Logger.getInstance(TimeTrackerWidgetService.class);

  private static final long TICK_DELAY_SECONDS = 1;

  private final Project project;
  private final TimeTrackerWidget widget;
  private final AtomicLong initProjectTime = new AtomicLong(0);
  private final AtomicLong initTotalTime = new AtomicLong(0);
  private LocalDate lastDate = LocalDate.now();
  private final SafeStopWatch projectStopWatch = SafeStopWatch.createStopped();
  private ScheduledFuture<?> ticker;

  public TimeTrackerWidgetService(Project project) {
    this.project = project;
    this.widget = new TimeTrackerWidget(project, this);
    startTicker();
  }

  public void initialize(long initialSeconds, long totalSeconds) {
    this.initProjectTime.set(initialSeconds);
    this.initTotalTime.set(totalSeconds);
    repaintWidget();
  }

  public void pause() {
    projectStopWatch.pause();
  }

  public void resume() {
    projectStopWatch.resume();
  }

  public long getTotalSeconds() {
    return initTotalTime.get() + GLOBAL_STOP_WATCH.getSeconds();
  }

  private long getProjectSeconds() {
    return initProjectTime.get() + projectStopWatch.getSeconds();
  }

  public String getFormattedProjectTime() {
    long seconds = getProjectSeconds();
    return formatTime(seconds);
  }

  public String getFormattedTotalTime() {
    long seconds = getTotalSeconds();
    return formatTime(seconds);
  }

  private String formatTime(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;

    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    } else {
      return String.format("%dm", minutes);
    }
  }

  public TimeTrackerWidget getWidget() {
    return widget;
  }

  private void startTicker() {
    if (ticker != null) {
      ticker.cancel(false);
    }

    ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
    ticker =
        executor.scheduleWithFixedDelay(
            this::tick, TICK_DELAY_SECONDS, TICK_DELAY_SECONDS, TimeUnit.SECONDS);
  }

  private void tick() {
    checkMidnightReset();
    repaintWidget();
  }

  private void checkMidnightReset() {
    LocalDate currentDate = LocalDate.now();
    if (!currentDate.equals(lastDate)) {
      LOG.info("Midnight detected for project: " + project.getName());

      initProjectTime.set(0);
      initTotalTime.set(0);
      projectStopWatch.reset();
      GLOBAL_STOP_WATCH.reset();
      lastDate = currentDate;
    }
  }

  private void repaintWidget() {
    ApplicationManager.getApplication().invokeLater(widget::updateText);
  }

  @Override
  public void dispose() {
    if (ticker != null) {
      ticker.cancel(false);
    }
  }
}
