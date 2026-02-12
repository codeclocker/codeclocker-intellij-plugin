package com.codeclocker.plugin.intellij.goal;

import static com.intellij.notification.NotificationType.INFORMATION;

import com.codeclocker.plugin.intellij.analytics.Analytics;
import com.codeclocker.plugin.intellij.analytics.AnalyticsEventType;
import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** Service for showing notifications when coding goals are reached. */
@Service(Service.Level.APP)
public final class GoalNotificationService {

  private final Object lock = new Object();
  private LocalDate dailyGoalReachedDate;
  private LocalDate weeklyGoalReachedWeekStart;
  private int lastNotifiedDailyGoalMinutes;
  private int lastNotifiedWeeklyGoalMinutes;

  // Per-project notification tracking
  private final Map<String, LocalDate> projectDailyGoalReachedDates = new HashMap<>();
  private final Map<String, LocalDate> projectWeeklyGoalReachedWeekStarts = new HashMap<>();
  private final Map<String, Integer> lastNotifiedProjectDailyGoals = new HashMap<>();
  private final Map<String, Integer> lastNotifiedProjectWeeklyGoals = new HashMap<>();

  /**
   * Check goal progress and show notifications if goals are reached. Should be called periodically
   * (e.g., from the widget tick).
   */
  public void checkAndNotify() {
    if (!GoalPersistence.isNotificationsEnabled()) {
      return;
    }

    GoalService goalService = ApplicationManager.getApplication().getService(GoalService.class);
    if (goalService == null) {
      return;
    }

    checkDailyGoal(goalService);
    checkWeeklyGoal(goalService);
  }

  private void checkDailyGoal(GoalService goalService) {
    if (!GoalPersistence.hasDailyGoal()) {
      return;
    }

    GoalProgress progress = goalService.getDailyProgress();
    if (!progress.isComplete()) {
      return;
    }

    int currentGoalMinutes = GoalPersistence.getDailyGoalMinutes();

    // Synchronize to prevent multiple notifications from concurrent project tickers
    synchronized (lock) {
      LocalDate today = LocalDate.now();

      // Reset flag if it's a new day
      if (dailyGoalReachedDate != null && !dailyGoalReachedDate.equals(today)) {
        dailyGoalReachedDate = null;
        lastNotifiedDailyGoalMinutes = 0;
      }

      // Reset flag if goal was increased since last notification
      if (dailyGoalReachedDate != null && currentGoalMinutes > lastNotifiedDailyGoalMinutes) {
        dailyGoalReachedDate = null;
      }

      // Already notified today for this goal
      if (dailyGoalReachedDate != null) {
        return;
      }

      dailyGoalReachedDate = today;
      lastNotifiedDailyGoalMinutes = currentGoalMinutes;
    }

    showDailyGoalNotification(progress);
  }

  private void checkWeeklyGoal(GoalService goalService) {
    if (!GoalPersistence.hasWeeklyGoal()) {
      return;
    }

    GoalProgress progress = goalService.getWeeklyProgress();
    if (!progress.isComplete()) {
      return;
    }

    int currentGoalMinutes = GoalPersistence.getWeeklyGoalMinutes();

    // Synchronize to prevent multiple notifications from concurrent project tickers
    synchronized (lock) {
      LocalDate today = LocalDate.now();
      LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);

      // Reset flag if it's a new week
      if (weeklyGoalReachedWeekStart != null
          && !weeklyGoalReachedWeekStart.equals(currentWeekStart)) {
        weeklyGoalReachedWeekStart = null;
        lastNotifiedWeeklyGoalMinutes = 0;
      }

      // Reset flag if goal was increased since last notification
      if (weeklyGoalReachedWeekStart != null
          && currentGoalMinutes > lastNotifiedWeeklyGoalMinutes) {
        weeklyGoalReachedWeekStart = null;
      }

      // Already notified this week for this goal
      if (weeklyGoalReachedWeekStart != null) {
        return;
      }

      weeklyGoalReachedWeekStart = currentWeekStart;
      lastNotifiedWeeklyGoalMinutes = currentGoalMinutes;
    }

    showWeeklyGoalNotification(progress);
  }

  /**
   * Check project-specific goal progress and show notifications if reached. Only checks if the
   * project has custom goals enabled.
   *
   * @param projectName the project name to check
   */
  public void checkAndNotifyForProject(@NotNull String projectName) {
    if (!ProjectGoalPersistence.hasCustomGoals(projectName)) {
      return;
    }

    if (!ProjectGoalPersistence.isProjectNotificationsEnabled(projectName)) {
      return;
    }

    GoalService goalService = ApplicationManager.getApplication().getService(GoalService.class);
    if (goalService == null) {
      return;
    }

    checkProjectDailyGoal(goalService, projectName);
    checkProjectWeeklyGoal(goalService, projectName);
  }

  private void checkProjectDailyGoal(GoalService goalService, String projectName) {
    int goalMinutes = ProjectGoalPersistence.getProjectDailyGoalMinutes(projectName);
    if (goalMinutes <= 0) {
      return;
    }

    GoalProgress progress = goalService.getProjectDailyProgress(projectName);
    if (!progress.isComplete()) {
      return;
    }

    synchronized (lock) {
      LocalDate today = LocalDate.now();
      LocalDate lastReached = projectDailyGoalReachedDates.get(projectName);

      // Reset flag if it's a new day
      if (lastReached != null && !lastReached.equals(today)) {
        projectDailyGoalReachedDates.remove(projectName);
        lastNotifiedProjectDailyGoals.remove(projectName);
        lastReached = null;
      }

      // Reset flag if goal was increased since last notification
      Integer lastNotifiedGoal = lastNotifiedProjectDailyGoals.get(projectName);
      if (lastReached != null && lastNotifiedGoal != null && goalMinutes > lastNotifiedGoal) {
        projectDailyGoalReachedDates.remove(projectName);
        lastReached = null;
      }

      // Already notified today for this goal
      if (lastReached != null) {
        return;
      }

      projectDailyGoalReachedDates.put(projectName, today);
      lastNotifiedProjectDailyGoals.put(projectName, goalMinutes);
    }

    showProjectDailyGoalNotification(projectName, progress);
  }

  private void checkProjectWeeklyGoal(GoalService goalService, String projectName) {
    int goalMinutes = ProjectGoalPersistence.getProjectWeeklyGoalMinutes(projectName);
    if (goalMinutes <= 0) {
      return;
    }

    GoalProgress progress = goalService.getProjectWeeklyProgress(projectName);
    if (!progress.isComplete()) {
      return;
    }

    synchronized (lock) {
      LocalDate today = LocalDate.now();
      LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
      LocalDate lastReachedWeekStart = projectWeeklyGoalReachedWeekStarts.get(projectName);

      // Reset flag if it's a new week
      if (lastReachedWeekStart != null && !lastReachedWeekStart.equals(currentWeekStart)) {
        projectWeeklyGoalReachedWeekStarts.remove(projectName);
        lastNotifiedProjectWeeklyGoals.remove(projectName);
        lastReachedWeekStart = null;
      }

      // Reset flag if goal was increased since last notification
      Integer lastNotifiedGoal = lastNotifiedProjectWeeklyGoals.get(projectName);
      if (lastReachedWeekStart != null
          && lastNotifiedGoal != null
          && goalMinutes > lastNotifiedGoal) {
        projectWeeklyGoalReachedWeekStarts.remove(projectName);
        lastReachedWeekStart = null;
      }

      // Already notified this week for this goal
      if (lastReachedWeekStart != null) {
        return;
      }

      projectWeeklyGoalReachedWeekStarts.put(projectName, currentWeekStart);
      lastNotifiedProjectWeeklyGoals.put(projectName, goalMinutes);
    }

    showProjectWeeklyGoalNotification(projectName, progress);
  }

  private void showDailyGoalNotification(GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(
                        "🎯 CodeClocker: Daily goal reached!",
                        "You've reached your daily coding goal of "
                            + formatGoalTime(progress.goalSeconds())
                            + ".",
                        INFORMATION)
                    .addAction(new SetNewGoalAction())
                    .notify(getCurrentProject()));
  }

  private void showWeeklyGoalNotification(GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(
                        "🏆 CodeClocker: Weekly goal reached!",
                        "You've reached your weekly coding goal of "
                            + formatGoalTime(progress.goalSeconds())
                            + ".",
                        INFORMATION)
                    .addAction(new SetNewGoalAction())
                    .notify(getCurrentProject()));
  }

  private void showProjectDailyGoalNotification(String projectName, GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(
                        "🎯 CodeClocker: project daily goal reached!",
                        "You've reached your daily coding goal of "
                            + formatGoalTime(progress.goalSeconds())
                            + " for "
                            + projectName
                            + ".",
                        INFORMATION)
                    .addAction(new SetProjectGoalAction(projectName))
                    .notify(getCurrentProject()));
  }

  private void showProjectWeeklyGoalNotification(String projectName, GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(
                        "🏆 CodeClocker: project weekly goal reached!",
                        "You've reached your weekly coding goal of "
                            + formatGoalTime(progress.goalSeconds())
                            + " for "
                            + projectName
                            + ".",
                        INFORMATION)
                    .addAction(new SetProjectGoalAction(projectName))
                    .notify(getCurrentProject()));
  }

  private static class SetNewGoalAction extends com.intellij.notification.NotificationAction {
    SetNewGoalAction() {
      super("Set New Goal");
    }

    @Override
    public void actionPerformed(
        com.intellij.openapi.actionSystem.AnActionEvent e,
        com.intellij.notification.Notification notification) {
      Analytics.track(AnalyticsEventType.SET_NEW_GOAL);
      notification.expire();
      GoalSettingsDialog.showDialog();
    }
  }

  private static class SetProjectGoalAction extends com.intellij.notification.NotificationAction {
    private final String projectName;

    SetProjectGoalAction(String projectName) {
      super("Set New Goal");
      this.projectName = projectName;
    }

    @Override
    public void actionPerformed(
        com.intellij.openapi.actionSystem.AnActionEvent e,
        com.intellij.notification.Notification notification) {
      Analytics.track(AnalyticsEventType.SET_NEW_GOAL);
      notification.expire();
      Project project = e.getProject();
      if (project != null) {
        ProjectGoalSettingsDialog.showDialog(project);
      }
    }
  }

  private String formatGoalTime(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;

    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    } else {
      return String.format("%dm", minutes);
    }
  }

  private static Project getCurrentProject() {
    DataContext dataContext = DataManager.getInstance().getDataContext(null);
    return dataContext.getData(CommonDataKeys.PROJECT);
  }
}
