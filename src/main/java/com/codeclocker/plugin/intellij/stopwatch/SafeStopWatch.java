package com.codeclocker.plugin.intellij.stopwatch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.time.StopWatch;

public class SafeStopWatch {

  private final AtomicReference<StopWatch> stopWatch;

  public SafeStopWatch(StopWatch stopWatch) {
    this.stopWatch = new AtomicReference<>(stopWatch);
  }

  public static SafeStopWatch createStarted() {
    return new SafeStopWatch(StopWatch.createStarted());
  }

  public static SafeStopWatch createStopped() {
    return new SafeStopWatch(StopWatch.create());
  }

  public long getSeconds() {
    return this.stopWatch.get().getTime(TimeUnit.SECONDS);
  }

  public void resume() {
    stopWatch.updateAndGet(
        time -> {
          if (!time.isStarted()) {
            time.start();
          } else if (time.isSuspended()) {
            time.resume();
          }
          return time;
        });
  }

  public void pause() {
    stopWatch.updateAndGet(
        time -> {
          if (!time.isSuspended() && time.isStarted()) {
            time.suspend();
          }
          return time;
        });
  }

  public void reset() {
    stopWatch.set(StopWatch.create());
  }

  public boolean isRunning() {
    return stopWatch.get().isStarted();
  }
}
