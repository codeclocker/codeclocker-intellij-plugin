package com.codeclocker.plugin.intellij.services;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TimeSpentPerFileLogger {

  private final Map<String, TimeSpentPerFileSample> timingByElement = new ConcurrentHashMap<>();
  private final AtomicReference<String> currentElement = new AtomicReference<>();

  public void compute(VirtualFile file, Consumer<TimeSpentPerFileSample> consumer) {
    this.timingByElement.compute(
        file.getName(),
        (name, sample) -> {
          if (sample == null) {
            sample = TimeSpentPerFileSample.create(file);
          }
          consumer.accept(sample);
          return sample;
        });
  }

  public void log(VirtualFile file) {
    String prevElement = this.currentElement.getAndSet(file.getName());

    if (prevElement != null && !Objects.equals(prevElement, currentElement)) {
      pauseWatchForPrevElement(prevElement);
    }

    timingByElement.compute(
        file.getName(),
        (name, sample) -> {
          if (sample == null) {
            return TimeSpentPerFileSample.create(file);
          }
          return sample.resumeSpendingTime();
        });
  }

  private void pauseWatchForPrevElement(String prevElement) {
    timingByElement.compute(
        prevElement,
        (name, sample) -> {
          if (sample == null) {
            return null;
          }

          sample.pauseSpendingTime();
          return sample;
        });
  }

  public void pauseDueToInactivity() {
    currentElement.updateAndGet(
        current -> {
          if (current == null) {
            return null;
          }

          timingByElement.compute(
              current,
              (name, sample) -> {
                if (sample != null) {
                  sample.pauseSpendingTime();
                }
                return sample;
              });

          return current;
        });
  }

  public Map<String, TimeSpentPerFileSample> getTimingByElement() {
    return timingByElement;
  }
}
