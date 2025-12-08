package com.codeclocker.plugin.intellij.services;

import com.codeclocker.plugin.intellij.stopwatch.SafeStopWatch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TimeSpentPerProjectLogger {

  private static final Logger LOG = Logger.getInstance(TimeSpentPerProjectLogger.class);

  public static final SafeStopWatch GLOBAL_STOP_WATCH = SafeStopWatch.createStopped();
  public static final AtomicLong GLOBAL_INIT_SECONDS = new AtomicLong();

  private final Map<String, TimeSpentPerProjectSample> timingByProject = new ConcurrentHashMap<>();
  private final AtomicReference<Project> currentProject = new AtomicReference<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public void log(Project project) {
    GLOBAL_STOP_WATCH.resume();
    Project prevProject = this.currentProject.getAndSet(project);

    if (prevProject != null && !Objects.equals(prevProject.getName(), project.getName())) {
      pauseWatchForPrevProject(prevProject);
    }

    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      timingByProject.compute(
          project.getName(),
          (name, sample) -> {
            if (project.isDisposed()) {
              return sample;
            }
            TimeTrackerWidgetService service = project.getService(TimeTrackerWidgetService.class);
            service.resume();
            if (sample == null) {
              return TimeSpentPerProjectSample.createStarted();
            }
            return sample.resume();
          });
    } finally {
      lock.unlock();
    }
  }

  private void pauseWatchForPrevProject(Project prevProject) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      timingByProject.compute(
          prevProject.getName(),
          (name, sample) -> {
            if (prevProject.isDisposed()) {
              if (sample != null) {
                sample.pause();
              }
              return sample;
            }
            TimeTrackerWidgetService service =
                prevProject.getService(TimeTrackerWidgetService.class);
            service.pause();
            if (sample == null) {
              return null;
            }

            sample.pause();
            return sample;
          });
    } finally {
      lock.unlock();
    }
  }

  public void pauseDueToInactivity() {
    GLOBAL_STOP_WATCH.pause();
    currentProject.updateAndGet(
        currentProject -> {
          if (currentProject == null) {
            return null;
          }

          Lock lock = readWriteLock.readLock();
          try {
            lock.lock();
            timingByProject.compute(
                currentProject.getName(),
                (name, sample) -> {
                  if (currentProject.isDisposed()) {
                    if (sample != null) {
                      sample.pause();
                    }
                    return sample;
                  }
                  TimeTrackerWidgetService service =
                      currentProject.getService(TimeTrackerWidgetService.class);
                  service.pause();
                  if (sample != null) {
                    sample.pause();
                  }
                  return sample;
                });

            return currentProject;
          } finally {
            lock.unlock();
          }
        });
  }

  public void pauseProject(Project project) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      timingByProject.computeIfPresent(
          project.getName(),
          (name, sample) -> {
            sample.pause();
            LOG.debug("Paused time tracking for closing project: " + name);
            return sample;
          });

      clearCurrentProject(project);
    } finally {
      lock.unlock();
    }
  }

  private void clearCurrentProject(Project project) {
    currentProject.updateAndGet(
        current ->
            current != null && Objects.equals(current.getName(), project.getName())
                ? null
                : current);
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

  /**
   * Validates that the global stopwatch time matches the sum of all per-project times. This helps
   * detect timing inconsistencies, data corruption, or race conditions.
   *
   * @return ValidationResult containing whether validation passed and details about any mismatch
   */
  public ValidationResult validateTimers() {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();

      long globalTime = GLOBAL_STOP_WATCH.getSeconds();
      long sumOfProjects =
          timingByProject.values().stream()
              .mapToLong(sample -> sample.timeSpent().getSeconds())
              .sum();

      long difference = Math.abs(globalTime - sumOfProjects);

      // Allow small differences (up to 2 seconds) due to timing precision and race conditions
      boolean isValid = difference <= 2;

      if (!isValid) {
        LOG.warn(
            String.format(
                "Timer mismatch detected! Global time: %ds, Sum of projects: %ds, Difference: %ds",
                globalTime, sumOfProjects, difference));

        // Log per-project breakdown for debugging
        if (LOG.isDebugEnabled()) {
          StringBuilder breakdown = new StringBuilder("Per-project timing breakdown:\n");
          timingByProject.forEach(
              (projectName, sample) -> {
                long projectSeconds = sample.timeSpent().getSeconds();
                breakdown.append(
                    String.format(
                        "  - %s: %ds (started at %d)\n",
                        projectName, projectSeconds, sample.samplingStartedAt()));
              });
          LOG.debug(breakdown.toString());
        }
      } else if (difference > 0) {
        LOG.debug(
            String.format(
                "Timer validation passed with minor difference: Global=%ds, Sum=%ds, Diff=%ds",
                globalTime, sumOfProjects, difference));
      }

      return new ValidationResult(isValid, globalTime, sumOfProjects, difference);

    } finally {
      lock.unlock();
    }
  }

  /** Result of timer validation containing timing details and whether validation passed. */
  public record ValidationResult(
      boolean isValid, long globalTimeSeconds, long sumOfProjectsSeconds, long differenceSeconds) {

    public String getSummary() {
      return String.format(
          "Global: %ds, Sum: %ds, Diff: %ds, Valid: %s",
          globalTimeSeconds, sumOfProjectsSeconds, differenceSeconds, isValid);
    }
  }
}
