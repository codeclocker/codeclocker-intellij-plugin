package com.codeclocker.plugin.intellij.services;

import com.codeclocker.plugin.intellij.stopwatch.SafeStopWatch;

public record TimeSpentPerProjectSample(long samplingStartedAt, SafeStopWatch timeSpent) {

  public static TimeSpentPerProjectSample createStarted() {
    return new TimeSpentPerProjectSample(System.currentTimeMillis(), SafeStopWatch.createStarted());
  }

  public TimeSpentPerProjectSample resume() {
    timeSpent.resume();

    return this;
  }

  public void pause() {
    timeSpent.pause();
  }

  public boolean isRunning() {
    return timeSpent.isRunning();
  }
}
