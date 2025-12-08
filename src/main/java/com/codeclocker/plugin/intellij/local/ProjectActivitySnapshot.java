package com.codeclocker.plugin.intellij.local;

/**
 * Snapshot of activity data for a single project. Stores coded time in seconds and VCS change
 * counts.
 */
public class ProjectActivitySnapshot {

  private long codedTimeSeconds;
  private long additions;
  private long removals;
  private boolean reported;

  public ProjectActivitySnapshot() {
    // Required for XML serialization
  }

  public ProjectActivitySnapshot(
      long codedTimeSeconds, long additions, long removals, boolean reported) {
    this.codedTimeSeconds = codedTimeSeconds;
    this.additions = additions;
    this.removals = removals;
    this.reported = reported;
  }

  public long getCodedTimeSeconds() {
    return codedTimeSeconds;
  }

  public void setCodedTimeSeconds(long codedTimeSeconds) {
    this.codedTimeSeconds = codedTimeSeconds;
  }

  public long getAdditions() {
    return additions;
  }

  public void setAdditions(long additions) {
    this.additions = additions;
  }

  public long getRemovals() {
    return removals;
  }

  public void setRemovals(long removals) {
    this.removals = removals;
  }

  public boolean isReported() {
    return reported;
  }

  public void setReported(boolean reported) {
    this.reported = reported;
  }
}
