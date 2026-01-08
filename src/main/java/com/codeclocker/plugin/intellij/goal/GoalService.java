package com.codeclocker.plugin.intellij.goal;

import com.codeclocker.plugin.intellij.local.LocalActivityDataProvider;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

/** Application-level service for calculating goal progress. */
@Service(Service.Level.APP)
public final class GoalService {

  /**
   * Get the current daily goal progress. Always returns progress using configured or default goal.
   */
  public GoalProgress getDailyProgress() {
    int goalMinutes = GoalPersistence.getDailyGoalMinutes();
    long goalSeconds = goalMinutes * 60L;
    long currentSeconds = getTotalSecondsToday();

    return GoalProgress.of(currentSeconds, goalSeconds);
  }

  /**
   * Get the current weekly goal progress. Always returns progress using configured or default goal.
   */
  public GoalProgress getWeeklyProgress() {
    int goalMinutes = GoalPersistence.getWeeklyGoalMinutes();
    long goalSeconds = goalMinutes * 60L;
    long currentSeconds = getTotalSecondsThisWeek();

    return GoalProgress.of(currentSeconds, goalSeconds);
  }

  /** Get total coded seconds for today from live tracking. */
  private long getTotalSecondsToday() {
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger == null) {
      return 0;
    }
    return logger.getGlobalAccumulatedToday();
  }

  /**
   * Get total coded seconds for the current week (Monday to today). Uses LocalActivityDataProvider
   * which returns data in local timezone.
   */
  private long getTotalSecondsThisWeek() {
    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    if (dataProvider == null) {
      return getTotalSecondsToday();
    }

    // Get week total from provider (includes historical data in local timezone)
    // Then add any unsaved deltas from today's live tracking
    long weekSeconds = dataProvider.getWeekTotalSeconds();

    // Add unsaved deltas that haven't been persisted yet
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger != null) {
      weekSeconds += logger.getGlobalUnsavedDelta();
    }

    return weekSeconds;
  }
}
