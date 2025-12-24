package com.codeclocker.plugin.intellij.goal;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Handles persistent storage of coding time goal settings. Goals are stored in minutes for easy UI
 * handling (hours + minutes).
 */
public class GoalPersistence {

  private static final String DAILY_GOAL_MINUTES = "com.codeclocker.goal.daily-minutes";
  private static final String WEEKLY_GOAL_MINUTES = "com.codeclocker.goal.weekly-minutes";
  private static final String GOALS_ENABLED = "com.codeclocker.goal.enabled";
  private static final String NOTIFICATIONS_ENABLED = "com.codeclocker.goal.notifications-enabled";

  private static final int DEFAULT_DAILY_GOAL_MINUTES = 60; // 1 hour
  private static final int DEFAULT_WEEKLY_GOAL_MINUTES = 300; // 5 hours

  /**
   * Get the daily coding time goal in minutes.
   *
   * @return goal in minutes (defaults to 60 minutes / 1 hour)
   */
  public static int getDailyGoalMinutes() {
    return PropertiesComponent.getInstance().getInt(DAILY_GOAL_MINUTES, DEFAULT_DAILY_GOAL_MINUTES);
  }

  /**
   * Set the daily coding time goal.
   *
   * @param minutes goal in minutes
   */
  public static void setDailyGoalMinutes(int minutes) {
    PropertiesComponent.getInstance()
        .setValue(DAILY_GOAL_MINUTES, minutes, DEFAULT_DAILY_GOAL_MINUTES);
  }

  /**
   * Get the weekly coding time goal in minutes.
   *
   * @return goal in minutes (defaults to 300 minutes / 5 hours)
   */
  public static int getWeeklyGoalMinutes() {
    return PropertiesComponent.getInstance()
        .getInt(WEEKLY_GOAL_MINUTES, DEFAULT_WEEKLY_GOAL_MINUTES);
  }

  /**
   * Set the weekly coding time goal.
   *
   * @param minutes goal in minutes
   */
  public static void setWeeklyGoalMinutes(int minutes) {
    PropertiesComponent.getInstance()
        .setValue(WEEKLY_GOAL_MINUTES, minutes, DEFAULT_WEEKLY_GOAL_MINUTES);
  }

  /**
   * Check if goal display is enabled.
   *
   * @return true if goals should be displayed
   */
  public static boolean isGoalsEnabled() {
    return PropertiesComponent.getInstance().getBoolean(GOALS_ENABLED, true);
  }

  /**
   * Enable or disable goal display.
   *
   * @param enabled true to show goals in UI
   */
  public static void setGoalsEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(GOALS_ENABLED, enabled, true);
  }

  /**
   * Check if a daily goal has been configured.
   *
   * @return true if daily goal is set and greater than 0
   */
  public static boolean hasDailyGoal() {
    return getDailyGoalMinutes() > 0;
  }

  /**
   * Check if a weekly goal has been configured.
   *
   * @return true if weekly goal is set and greater than 0
   */
  public static boolean hasWeeklyGoal() {
    return getWeeklyGoalMinutes() > 0;
  }

  /**
   * Check if goal notifications are enabled.
   *
   * @return true if notifications should be shown when goals are reached
   */
  public static boolean isNotificationsEnabled() {
    return PropertiesComponent.getInstance().getBoolean(NOTIFICATIONS_ENABLED, true);
  }

  /**
   * Enable or disable goal notifications.
   *
   * @param enabled true to show notifications when goals are reached
   */
  public static void setNotificationsEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(NOTIFICATIONS_ENABLED, enabled, true);
  }
}
