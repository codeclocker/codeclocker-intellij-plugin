package com.codeclocker.plugin.intellij.local;

/** Record of time spent on a specific git branch during an hour. */
public class BranchActivityRecord {

  private String branchName;
  private long activeSeconds;

  public BranchActivityRecord() {
    // Required for XML serialization
  }

  public BranchActivityRecord(String branchName, long activeSeconds) {
    this.branchName = branchName;
    this.activeSeconds = activeSeconds;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public long getActiveSeconds() {
    return activeSeconds;
  }

  public void setActiveSeconds(long activeSeconds) {
    this.activeSeconds = activeSeconds;
  }
}
