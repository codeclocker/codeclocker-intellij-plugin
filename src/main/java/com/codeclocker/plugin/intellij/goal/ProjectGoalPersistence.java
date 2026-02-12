package com.codeclocker.plugin.intellij.goal;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Handles persistent storage of per-project coding time goal settings. Per-project goals override
 * global goals when enabled.
 */
public class ProjectGoalPersistence {

  private static final String PREFIX = "com.codeclocker.goal.project.";
  private static final String DAILY_SUFFIX = ".daily-minutes";
  private static final String WEEKLY_SUFFIX = ".weekly-minutes";
  private static final String ENABLED_SUFFIX = ".enabled";
  private static final String NOTIFICATIONS_SUFFIX = ".notifications-enabled";

  /**
   * Sanitize project name for use in property keys. Replaces characters that could cause issues in
   * property keys.
   */
  private static String sanitizeProjectName(String projectName) {
    return projectName.replace(".", "_").replace("/", "_").replace("\\", "_");
  }

  private static String dailyKey(String projectName) {
    return PREFIX + sanitizeProjectName(projectName) + DAILY_SUFFIX;
  }

  private static String weeklyKey(String projectName) {
    return PREFIX + sanitizeProjectName(projectName) + WEEKLY_SUFFIX;
  }

  private static String enabledKey(String projectName) {
    return PREFIX + sanitizeProjectName(projectName) + ENABLED_SUFFIX;
  }

  private static String notificationsKey(String projectName) {
    return PREFIX + sanitizeProjectName(projectName) + NOTIFICATIONS_SUFFIX;
  }

  /**
   * Check if project has custom goals enabled.
   *
   * @param projectName the project name
   * @return true if project uses custom goals instead of global goals
   */
  public static boolean hasCustomGoals(String projectName) {
    return PropertiesComponent.getInstance().getBoolean(enabledKey(projectName), false);
  }

  /**
   * Enable or disable custom project goals.
   *
   * @param projectName the project name
   * @param enabled true to use custom goals, false to use global goals
   */
  public static void setCustomGoalsEnabled(String projectName, boolean enabled) {
    PropertiesComponent.getInstance().setValue(enabledKey(projectName), enabled, false);
  }

  /**
   * Get effective daily goal for a project. Returns custom goal if enabled, otherwise global goal.
   *
   * @param projectName the project name
   * @return daily goal in minutes
   */
  public static int getEffectiveDailyGoalMinutes(String projectName) {
    if (hasCustomGoals(projectName)) {
      return getProjectDailyGoalMinutes(projectName);
    }
    return GoalPersistence.getDailyGoalMinutes();
  }

  /**
   * Get effective weekly goal for a project. Returns custom goal if enabled, otherwise global goal.
   *
   * @param projectName the project name
   * @return weekly goal in minutes
   */
  public static int getEffectiveWeeklyGoalMinutes(String projectName) {
    if (hasCustomGoals(projectName)) {
      return getProjectWeeklyGoalMinutes(projectName);
    }
    return GoalPersistence.getWeeklyGoalMinutes();
  }

  /**
   * Get custom daily goal for a project.
   *
   * @param projectName the project name
   * @return custom daily goal in minutes, or global default if not set
   */
  public static int getProjectDailyGoalMinutes(String projectName) {
    return PropertiesComponent.getInstance()
        .getInt(dailyKey(projectName), GoalPersistence.getDailyGoalMinutes());
  }

  /**
   * Set custom daily goal for a project.
   *
   * @param projectName the project name
   * @param minutes goal in minutes
   */
  public static void setProjectDailyGoalMinutes(String projectName, int minutes) {
    PropertiesComponent.getInstance()
        .setValue(dailyKey(projectName), minutes, GoalPersistence.getDailyGoalMinutes());
  }

  /**
   * Get custom weekly goal for a project.
   *
   * @param projectName the project name
   * @return custom weekly goal in minutes, or global default if not set
   */
  public static int getProjectWeeklyGoalMinutes(String projectName) {
    return PropertiesComponent.getInstance()
        .getInt(weeklyKey(projectName), GoalPersistence.getWeeklyGoalMinutes());
  }

  /**
   * Set custom weekly goal for a project.
   *
   * @param projectName the project name
   * @param minutes goal in minutes
   */
  public static void setProjectWeeklyGoalMinutes(String projectName, int minutes) {
    PropertiesComponent.getInstance()
        .setValue(weeklyKey(projectName), minutes, GoalPersistence.getWeeklyGoalMinutes());
  }

  /**
   * Check if per-project goal notifications are enabled.
   *
   * @param projectName the project name
   * @return true if project notifications should be shown when goals are reached
   */
  public static boolean isProjectNotificationsEnabled(String projectName) {
    return PropertiesComponent.getInstance().getBoolean(notificationsKey(projectName), true);
  }

  /**
   * Enable or disable per-project goal notifications.
   *
   * @param projectName the project name
   * @param enabled true to show notifications when project goals are reached
   */
  public static void setProjectNotificationsEnabled(String projectName, boolean enabled) {
    PropertiesComponent.getInstance().setValue(notificationsKey(projectName), enabled, true);
  }
}
