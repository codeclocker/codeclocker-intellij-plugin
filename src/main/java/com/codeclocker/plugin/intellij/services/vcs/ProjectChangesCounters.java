package com.codeclocker.plugin.intellij.services.vcs;

import java.util.concurrent.atomic.AtomicLong;

public class ProjectChangesCounters {

  private final AtomicLong additions = new AtomicLong(0);
  private final AtomicLong removals = new AtomicLong(0);

  public ProjectChangesCounters(long initialAdditions, long initialRemovals) {
    this.additions.set(initialAdditions);
    this.removals.set(initialRemovals);
  }

  public void initialize(long initialAdditions, long initialRemovals) {
    this.additions.set(initialAdditions);
    this.removals.set(initialRemovals);
  }

  public AtomicLong additions() {
    return additions;
  }

  public AtomicLong removals() {
    return removals;
  }
}
