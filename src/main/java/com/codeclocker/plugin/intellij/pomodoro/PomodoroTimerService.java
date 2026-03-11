package com.codeclocker.plugin.intellij.pomodoro;

import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

@Service(Service.Level.APP)
public final class PomodoroTimerService {

  private PomodoroState state = PomodoroState.IDLE;
  private long workSecondsAccumulated;
  private long breakSecondsRemaining;
  private int completedCycles;
  private long lastActivityCheckGlobalSeconds;
  private long lastTickTimestampMillis;

  public synchronized void start() {
    state = PomodoroState.WORKING;
    PomodoroPersistence.setEnabled(true);
    PomodoroPersistence.setWasActiveOnShutdown(true);
    workSecondsAccumulated = 0;

    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    lastActivityCheckGlobalSeconds = logger != null ? logger.getGlobalAccumulatedToday() : 0;
  }

  public synchronized void stop() {
    state = PomodoroState.IDLE;
    PomodoroPersistence.setEnabled(false);
    PomodoroPersistence.setWasActiveOnShutdown(false);
    workSecondsAccumulated = 0;
    breakSecondsRemaining = 0;
    completedCycles = 0;
  }

  public synchronized void startBreak(boolean isLongBreak) {
    if (state != PomodoroState.IDLE) {
      return;
    }
    state = PomodoroState.BREAK;
    PomodoroPersistence.setWasActiveOnShutdown(true);
    breakSecondsRemaining =
        (isLongBreak
                ? PomodoroPersistence.getLongBreakMinutes()
                : PomodoroPersistence.getShortBreakMinutes())
            * 60L;
  }

  public synchronized void skipBreak() {
    if (state != PomodoroState.BREAK && state != PomodoroState.IDLE) {
      return;
    }
    startNextWorkInterval();
  }

  public synchronized void tick() {
    long now = System.currentTimeMillis();
    if (now - lastTickTimestampMillis < 500) {
      return;
    }
    lastTickTimestampMillis = now;

    if (state == PomodoroState.WORKING) {
      tickWorking();
    } else if (state == PomodoroState.BREAK) {
      tickBreak();
    }
  }

  private void tickWorking() {
    if (PomodoroPersistence.isUseCodingTime()) {
      tickWorkingCodingTime();
    } else {
      tickWorkingRealClock();
    }
  }

  private void tickWorkingRealClock() {
    workSecondsAccumulated++;

    long targetSeconds = PomodoroPersistence.getWorkMinutes() * 60L;
    if (workSecondsAccumulated >= targetSeconds) {
      completedCycles++;
      transitionToBreak();
    }
  }

  private void tickWorkingCodingTime() {
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger == null) {
      return;
    }

    long currentGlobalSeconds = logger.getGlobalAccumulatedToday();
    long delta = currentGlobalSeconds - lastActivityCheckGlobalSeconds;
    if (delta < 0) {
      delta = 0;
    }
    lastActivityCheckGlobalSeconds = currentGlobalSeconds;
    workSecondsAccumulated += delta;

    long targetSeconds = PomodoroPersistence.getWorkMinutes() * 60L;
    if (workSecondsAccumulated >= targetSeconds) {
      completedCycles++;
      transitionToBreak();
    }
  }

  private void transitionToBreak() {
    boolean isLongBreak =
        completedCycles > 0
            && completedCycles % PomodoroPersistence.getCyclesBeforeLongBreak() == 0;

    PomodoroNotificationService.notifyBreakTime(isLongBreak, completedCycles);

    if (PomodoroPersistence.isAutoStartBreak()) {
      state = PomodoroState.BREAK;
      PomodoroPersistence.setWasActiveOnShutdown(true);
      breakSecondsRemaining =
          (isLongBreak
                  ? PomodoroPersistence.getLongBreakMinutes()
                  : PomodoroPersistence.getShortBreakMinutes())
              * 60L;
    } else {
      state = PomodoroState.IDLE;
      PomodoroPersistence.setWasActiveOnShutdown(false);
    }
  }

  private void tickBreak() {
    breakSecondsRemaining--;
    if (breakSecondsRemaining <= 0) {
      breakSecondsRemaining = 0;
      state = PomodoroState.IDLE;
      PomodoroPersistence.setWasActiveOnShutdown(false);
      PomodoroNotificationService.notifyBreakOver();
    }
  }

  private void startNextWorkInterval() {
    state = PomodoroState.WORKING;
    PomodoroPersistence.setWasActiveOnShutdown(true);
    workSecondsAccumulated = 0;
    breakSecondsRemaining = 0;

    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    lastActivityCheckGlobalSeconds = logger != null ? logger.getGlobalAccumulatedToday() : 0;
  }

  public synchronized void resetActivityBaseline() {
    lastActivityCheckGlobalSeconds = 0;
  }

  public synchronized PomodoroState getState() {
    return state;
  }

  public synchronized long getWorkSecondsRemaining() {
    long targetSeconds = PomodoroPersistence.getWorkMinutes() * 60L;
    return Math.max(0, targetSeconds - workSecondsAccumulated);
  }

  public synchronized long getBreakSecondsRemaining() {
    return breakSecondsRemaining;
  }

  public synchronized int getCompletedCycles() {
    return completedCycles;
  }

  public String getFormattedWorkRemaining() {
    return formatSeconds(getWorkSecondsRemaining());
  }

  public String getFormattedBreakRemaining() {
    return formatSeconds(getBreakSecondsRemaining());
  }

  private static String formatSeconds(long seconds) {
    long totalMinutes = (long) Math.ceil(seconds / 60.0);
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;

    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    }
    return String.format("%dm", minutes);
  }
}
