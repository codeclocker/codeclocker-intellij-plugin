package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_REMOVALS;

import com.codeclocker.plugin.intellij.goal.GoalNotificationService;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.widget.TimeTrackerWidget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Per-project service that manages the time tracker widget display. Reads time data from
 * TimeSpentPerProjectLogger (the single source of truth).
 */
public class TimeTrackerWidgetService implements Disposable {

  private static final Logger LOG = Logger.getInstance(TimeTrackerWidgetService.class);

  private static final long TICK_DELAY_SECONDS = 1;

  private final Project project;
  private final TimeTrackerWidget widget;
  private final TimeSpentPerProjectLogger logger;

  private ScheduledFuture<?> ticker;

  public TimeTrackerWidgetService(Project project) {
    this.project = project;
    this.widget = new TimeTrackerWidget(project, this);
    this.logger = ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);

    startTicker();

    // Force an initial repaint to ensure the widget shows current data
    ApplicationManager.getApplication().invokeLater(this::repaintWidget);
  }

  /**
   * Get total seconds for today across all projects. Reads directly from the logger which is the
   * single source of truth.
   */
  public long getTotalSeconds() {
    if (logger == null) {
      return 0;
    }
    return logger.getGlobalAccumulatedToday();
  }

  /** Get seconds for this specific project today. Reads directly from the logger. */
  public long getProjectSeconds() {
    if (logger == null) {
      return 0;
    }
    return logger.getProjectAccumulatedToday(project.getName());
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
    checkGoalNotifications();
    repaintWidget();
  }

  private void checkGoalNotifications() {
    GoalNotificationService notificationService =
        ApplicationManager.getApplication().getService(GoalNotificationService.class);
    if (notificationService != null) {
      notificationService.checkAndNotify();
      notificationService.checkAndNotifyForProject(project.getName());
    }
  }

  private void checkMidnightReset() {
    if (logger != null && logger.hasMidnightPassed()) {
      LOG.info("Midnight detected for project: " + project.getName());

      // Logger handles its own reset, we just need to reset VCS counters
      GLOBAL_ADDITIONS.set(0);
      GLOBAL_REMOVALS.set(0);

      // Reset per-project VCS changes counters
      ChangesActivityTracker changesTracker =
          ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
      if (changesTracker != null) {
        changesTracker.clearAllProjectChanges();
      }

      // Trigger the logger to reset (it checks internally)
      logger.resetForNewDay();
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
