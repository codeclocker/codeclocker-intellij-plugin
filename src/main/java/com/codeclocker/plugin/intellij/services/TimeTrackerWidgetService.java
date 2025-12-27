package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_INIT_SECONDS;
import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_REMOVALS;

import com.codeclocker.plugin.intellij.goal.GoalNotificationService;
import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.stopwatch.SafeStopWatch;
import com.codeclocker.plugin.intellij.widget.TimeTrackerWidget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
  private final SafeStopWatch projectStopWatch = SafeStopWatch.createStopped();

  private LocalDate lastDate = LocalDate.now();
  private ScheduledFuture<?> ticker;

  public TimeTrackerWidgetService(Project project) {
    this.project = project;
    this.widget = new TimeTrackerWidget(project, this);

    // Initialize project time from local state for late-opened projects
    initializeProjectTimeFromLocalState();

    startTicker();

    // Force an initial repaint to ensure the widget shows current data
    // This handles cases where the project is opened after the global initialization
    ApplicationManager.getApplication().invokeLater(this::repaintWidget);
  }

  /**
   * Initialize project-specific time from local state. This ensures projects opened after the
   * global initialization still get their correct per-project time.
   */
  private void initializeProjectTimeFromLocalState() {
    try {
      LocalStateRepository localState =
          ApplicationManager.getApplication().getService(LocalStateRepository.class);
      if (localState == null) {
        return;
      }

      String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      String projectName = project.getName();
      long totalProjectSeconds = 0;

      for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry :
          localState.getAllData().entrySet()) {
        if (!hourEntry.getKey().startsWith(todayPrefix)) {
          continue;
        }

        ProjectActivitySnapshot snapshot = hourEntry.getValue().get(projectName);
        if (snapshot != null) {
          totalProjectSeconds += snapshot.getCodedTimeSeconds();
        }
      }

      if (totalProjectSeconds > 0) {
        LOG.debug(
            "Initialized project {} with {}s from local state", projectName, totalProjectSeconds);
        this.initProjectTime.set(totalProjectSeconds);
      }
    } catch (Exception e) {
      LOG.warn("Failed to initialize project time from local state", e);
    }
  }

  public void initialize(long initialSeconds) {
    this.initProjectTime.set(initialSeconds);
    this.projectStopWatch.reset();
    repaintWidget();
  }

  public void pause() {
    projectStopWatch.pause();
  }

  public void resume() {
    projectStopWatch.resume();
  }

  public long getTotalSeconds() {
    return GLOBAL_INIT_SECONDS.get() + GLOBAL_STOP_WATCH.getSeconds();
  }

  public long getProjectSeconds() {
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
    checkGoalNotifications();
    repaintWidget();
  }

  private void checkGoalNotifications() {
    GoalNotificationService notificationService =
        ApplicationManager.getApplication().getService(GoalNotificationService.class);
    if (notificationService != null) {
      notificationService.checkAndNotify();
    }
  }

  private void checkMidnightReset() {
    LocalDate currentDate = LocalDate.now();
    if (!currentDate.equals(lastDate)) {
      LOG.info("Midnight detected for project: " + project.getName());

      initProjectTime.set(0);
      GLOBAL_INIT_SECONDS.set(0);
      projectStopWatch.reset();
      GLOBAL_ADDITIONS.set(0);
      GLOBAL_REMOVALS.set(0);
      GLOBAL_STOP_WATCH.reset();

      // Reset per-project VCS changes counters
      ChangesActivityTracker changesTracker =
          ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
      if (changesTracker != null) {
        changesTracker.clearAllProjectChanges();
      }

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
