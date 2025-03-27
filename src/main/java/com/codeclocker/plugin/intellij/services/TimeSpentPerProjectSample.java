package com.codeclocker.plugin.intellij.services;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.time.StopWatch;

public record TimeSpentPerProjectSample(
    long samplingStartedAt, AtomicReference<StopWatch> timeSpent) {

  public static TimeSpentPerProjectSample create() {
    return new TimeSpentPerProjectSample(
        System.currentTimeMillis(), new AtomicReference<>(StopWatch.createStarted()));
  }

  public TimeSpentPerProjectSample resumeSpendingTime() {
    timeSpent.updateAndGet(
        time -> {
          if (time.isSuspended()) {
            time.resume();
          }
          return time;
        });

    return this;
  }

  public void pauseSpendingTime() {
    timeSpent.updateAndGet(
        time -> {
          if (!time.isSuspended()) {
            time.suspend();
          }
          return time;
        });
  }
}
