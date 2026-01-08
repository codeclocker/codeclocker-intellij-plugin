package com.codeclocker.plugin.intellij.local;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot of activity data for a single project. Stores coded time in seconds, VCS change counts,
 * branch activity, and commit records.
 */
public class ProjectActivitySnapshot {

  private String recordId;
  private long codedTimeSeconds;
  private long additions;
  private long removals;
  private boolean reported;
  private List<BranchActivityRecord> branchActivity = new ArrayList<>();
  private List<CommitRecord> commits = new ArrayList<>();

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

  public List<BranchActivityRecord> getBranchActivity() {
    return branchActivity;
  }

  public void setBranchActivity(List<BranchActivityRecord> branchActivity) {
    this.branchActivity = branchActivity != null ? branchActivity : new ArrayList<>();
  }

  public List<CommitRecord> getCommits() {
    return commits;
  }

  public void setCommits(List<CommitRecord> commits) {
    this.commits = commits != null ? commits : new ArrayList<>();
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  /** Ensures this snapshot has a recordId, generating one if not present. */
  public void ensureRecordId() {
    if (recordId == null || recordId.isEmpty()) {
      recordId = UUID.randomUUID().toString();
    }
  }
}
