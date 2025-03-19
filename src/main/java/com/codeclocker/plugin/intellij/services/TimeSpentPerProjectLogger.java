package com.codeclocker.plugin.intellij.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TimeSpentPerProjectLogger {

  private final Map<String, TimeSpentPerProjectSample> timingByProject = new ConcurrentHashMap<>();
  private final AtomicReference<String> currentElement = new AtomicReference<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public void log(String project) {
    String prevElement = this.currentElement.getAndSet(project);

    if (prevElement != null && !Objects.equals(prevElement, currentElement.get())) {
      pauseWatchForPrevElement(prevElement);
    }

    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      timingByProject.compute(
          project,
          (name, sample) -> {
            if (sample == null) {
              return TimeSpentPerProjectSample.create();
            }
            return sample.resumeSpendingTime();
          });
    } finally {
      lock.unlock();
    }
  }

  private void pauseWatchForPrevElement(String prevElement) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      timingByProject.compute(
          prevElement,
          (name, sample) -> {
            if (sample == null) {
              return null;
            }

            sample.pauseSpendingTime();
            return sample;
          });
    } finally {
      lock.unlock();
    }
  }

  public void pauseDueToInactivity() {
    currentElement.updateAndGet(
        current -> {
          if (current == null) {
            return null;
          }

          Lock lock = readWriteLock.readLock();
          try {
            lock.lock();
            timingByProject.compute(
                current,
                (name, sample) -> {
                  if (sample != null) {
                    sample.pauseSpendingTime();
                  }
                  return sample;
                });

            return current;
          } finally {
            lock.unlock();
          }
        });
  }

  public Map<String, TimeSpentPerProjectSample> drain() {
    Lock lock = readWriteLock.writeLock();
    try {
      lock.lock();

      Map<String, TimeSpentPerProjectSample> drain = new HashMap<>(timingByProject);
      timingByProject.clear();

      return drain;
    } finally {
      lock.unlock();
    }
  }
}
