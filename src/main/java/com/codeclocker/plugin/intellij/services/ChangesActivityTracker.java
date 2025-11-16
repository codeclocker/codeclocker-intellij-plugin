package com.codeclocker.plugin.intellij.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChangesActivityTracker {

  public static final AtomicLong GLOBAL_ADDITIONS = new AtomicLong(0);
  public static final AtomicLong GLOBAL_REMOVALS = new AtomicLong(0);

  private final Map<String, Map<String, ChangesSample>> fileNameByChangesSample =
      new ConcurrentHashMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public void incrementAdditions(
      String project, String filePath, String extension, long additions) {
    Lock lock = readWriteLock.readLock();
    try {
      lock.lock();
      GLOBAL_ADDITIONS.addAndGet(additions);

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
}
