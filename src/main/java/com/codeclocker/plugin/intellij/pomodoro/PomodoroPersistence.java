package com.codeclocker.plugin.intellij.pomodoro;

import com.intellij.ide.util.PropertiesComponent;

public class PomodoroPersistence {

  private static final String ENABLED = "com.codeclocker.pomodoro.enabled";
  private static final String WORK_MINUTES = "com.codeclocker.pomodoro.work-minutes";
  private static final String SHORT_BREAK_MINUTES = "com.codeclocker.pomodoro.short-break-minutes";
  private static final String LONG_BREAK_MINUTES = "com.codeclocker.pomodoro.long-break-minutes";
  private static final String CYCLES_BEFORE_LONG_BREAK =
      "com.codeclocker.pomodoro.cycles-before-long-break";
  private static final String AUTO_START_BREAK = "com.codeclocker.pomodoro.auto-start-break";
  private static final String NOTIFICATIONS_ENABLED =
      "com.codeclocker.pomodoro.notifications-enabled";
  private static final String USE_CODING_TIME = "com.codeclocker.pomodoro.use-coding-time";
  private static final String WAS_ACTIVE_ON_SHUTDOWN =
      "com.codeclocker.pomodoro.was-active-on-shutdown";

  private static final int DEFAULT_WORK_MINUTES = 25;
  private static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
  private static final int DEFAULT_LONG_BREAK_MINUTES = 15;
  private static final int DEFAULT_CYCLES_BEFORE_LONG_BREAK = 4;

  public static boolean isEnabled() {
    return PropertiesComponent.getInstance().getBoolean(ENABLED, false);
  }

  public static void setEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(ENABLED, enabled, false);
  }

  public static int getWorkMinutes() {
    return PropertiesComponent.getInstance().getInt(WORK_MINUTES, DEFAULT_WORK_MINUTES);
  }

  public static void setWorkMinutes(int minutes) {
    PropertiesComponent.getInstance().setValue(WORK_MINUTES, minutes, DEFAULT_WORK_MINUTES);
  }

  public static int getShortBreakMinutes() {
    return PropertiesComponent.getInstance()
        .getInt(SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES);
  }

  public static void setShortBreakMinutes(int minutes) {
    PropertiesComponent.getInstance()
        .setValue(SHORT_BREAK_MINUTES, minutes, DEFAULT_SHORT_BREAK_MINUTES);
  }

  public static int getLongBreakMinutes() {
    return PropertiesComponent.getInstance().getInt(LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES);
  }

  public static void setLongBreakMinutes(int minutes) {
    PropertiesComponent.getInstance()
        .setValue(LONG_BREAK_MINUTES, minutes, DEFAULT_LONG_BREAK_MINUTES);
  }

  public static int getCyclesBeforeLongBreak() {
    return PropertiesComponent.getInstance()
        .getInt(CYCLES_BEFORE_LONG_BREAK, DEFAULT_CYCLES_BEFORE_LONG_BREAK);
  }

  public static void setCyclesBeforeLongBreak(int cycles) {
    PropertiesComponent.getInstance()
        .setValue(CYCLES_BEFORE_LONG_BREAK, cycles, DEFAULT_CYCLES_BEFORE_LONG_BREAK);
  }

  public static boolean isAutoStartBreak() {
    return PropertiesComponent.getInstance().getBoolean(AUTO_START_BREAK, false);
  }

  public static void setAutoStartBreak(boolean autoStart) {
    PropertiesComponent.getInstance().setValue(AUTO_START_BREAK, autoStart, false);
  }

  public static boolean isNotificationsEnabled() {
    return PropertiesComponent.getInstance().getBoolean(NOTIFICATIONS_ENABLED, true);
  }

  public static void setNotificationsEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(NOTIFICATIONS_ENABLED, enabled, true);
  }

  public static boolean isUseCodingTime() {
    return PropertiesComponent.getInstance().getBoolean(USE_CODING_TIME, false);
  }

  public static void setUseCodingTime(boolean useCodingTime) {
    PropertiesComponent.getInstance().setValue(USE_CODING_TIME, useCodingTime, false);
  }

  public static boolean wasActiveOnShutdown() {
    return PropertiesComponent.getInstance().getBoolean(WAS_ACTIVE_ON_SHUTDOWN, false);
  }

  public static void setWasActiveOnShutdown(boolean active) {
    PropertiesComponent.getInstance().setValue(WAS_ACTIVE_ON_SHUTDOWN, active, false);
  }
}
