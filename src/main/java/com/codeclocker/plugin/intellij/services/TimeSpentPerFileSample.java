package com.codeclocker.plugin.intellij.services;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.time.StopWatch;

public record TimeSpentPerFileSample(
    long samplingStartedAt,
    AtomicReference<StopWatch> timeSpent,
    AtomicLong additions,
    AtomicLong removals,
    Map<String, String> metadata) {

  public static TimeSpentPerFileSample create(VirtualFile file) {
    return new TimeSpentPerFileSample(
        System.currentTimeMillis(),
        new AtomicReference<>(StopWatch.createStarted()),
        new AtomicLong(),
        new AtomicLong(),
        Map.of("fileType", file.getFileType().getName()));
  }

  public TimeSpentPerFileSample resumeSpendingTime() {
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

  public void incrementAdditions(long additions) {
    this.additions.getAndUpdate(prev -> prev + additions);
  }

  public void incrementRemovals(long removals) {
    this.removals.getAndUpdate(prev -> prev + removals);
  }
}
