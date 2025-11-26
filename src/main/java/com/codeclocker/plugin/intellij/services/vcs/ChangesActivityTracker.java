package com.codeclocker.plugin.intellij.services.vcs;

import com.codeclocker.plugin.intellij.services.ChangesSample;
import com.intellij.openapi.diagnostic.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChangesActivityTracker {

  private static final Logger LOG = Logger.getInstance(ChangesActivityTracker.class);

  public static final AtomicLong GLOBAL_ADDITIONS = new AtomicLong(0);
  public static final AtomicLong GLOBAL_REMOVALS = new AtomicLong(0);

  private final Map<String, Map<String, ChangesSample>> fileNameByChangesSample =
      new ConcurrentHashMap<>();
  private final Map<String, ProjectChangesCounters> projectChangesCounters =
      new ConcurrentHashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public void incrementAdditions(
      String project, String filePath, String extension, long additions) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      GLOBAL_ADDITIONS.addAndGet(additions);

      // Update per-project counters
      projectChangesCounters
          .computeIfAbsent(project, p -> new ProjectChangesCounters(0, 0))
          .additions()
          .addAndGet(additions);

      fileNameByChangesSample
          .computeIfAbsent(project, p -> new ConcurrentHashMap<>())
          .computeIfAbsent(filePath, f -> ChangesSample.create(extension))
          .incrementAdditions(additions);
    } finally {
      lock.unlock();
    }
  }

  public void incrementRemovals(String project, String fileName, String extension, long removals) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      GLOBAL_REMOVALS.addAndGet(removals);

      // Update per-project counters
      projectChangesCounters
          .computeIfAbsent(project, p -> new ProjectChangesCounters(0, 0))
          .removals()
          .addAndGet(removals);

      fileNameByChangesSample
          .computeIfAbsent(project, p -> new ConcurrentHashMap<>())
          .computeIfAbsent(fileName, f -> ChangesSample.create(extension))
          .incrementRemovals(removals);
    } finally {
      lock.unlock();
    }
  }

  public Map<String, Map<String, ChangesSample>> drain() {
    Lock lock = readWriteLock.writeLock();
    try {
      lock.lock();

      Map<String, Map<String, ChangesSample>> drain = new HashMap<>(fileNameByChangesSample);
      fileNameByChangesSample.clear();
      return drain;
    } finally {
      lock.unlock();
    }
  }

  public ProjectChangesCounters getProjectChanges(String projectName) {
    ProjectChangesCounters counters = projectChangesCounters.get(projectName);
    if (counters == null) {
      return new ProjectChangesCounters(0, 0);
    }
    return new ProjectChangesCounters(counters.additions().get(), counters.removals().get());
  }

  public void initializeProjectChanges(String projectName, long additions, long removals) {
    projectChangesCounters
        .computeIfAbsent(projectName, p -> new ProjectChangesCounters(additions, removals))
        .initialize(additions, removals);
    LOG.debug(
        "Initialized VCS counters for project {} with additions: {}, removals: {}",
        projectName,
        additions,
        removals);
  }

  public void clearAllProjectChanges() {
    LOG.debug("Clearing all per-project VCS counters");
    projectChangesCounters.clear();
  }
}
