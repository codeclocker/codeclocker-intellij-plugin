package com.codeclocker.plugin.intellij.goal;

import static com.intellij.notification.NotificationType.INFORMATION;

import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Service for showing notifications when coding goals are reached. */
@Service(Service.Level.APP)
public final class GoalNotificationService {

  private final Object lock = new Object();
  private LocalDate dailyGoalReachedDate;
  private LocalDate weeklyGoalReachedWeekStart;
  private int lastNotifiedDailyGoalMinutes;
  private int lastNotifiedWeeklyGoalMinutes;

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

  private void showDailyGoalNotification(GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              NotificationGroupManager.getInstance()
                  .getNotificationGroup("CodeClocker")
                  .createNotification(
                      "🎯 CodeClocker: Daily goal reached!",
                      "You've reached your daily coding goal of "
                          + formatGoalTime(progress.goalSeconds())
                          + ".",
                      INFORMATION)
                  .notify(getCurrentProject());
            });
  }

  private void showWeeklyGoalNotification(GoalProgress progress) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              NotificationGroupManager.getInstance()
                  .getNotificationGroup("CodeClocker")
                  .createNotification(
                      "🏆 CodeClocker: Weekly goal reached!",
                      "You've reached your weekly coding goal of "
                          + formatGoalTime(progress.goalSeconds())
                          + ".",
                      INFORMATION)
                  .notify(getCurrentProject());
            });
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
