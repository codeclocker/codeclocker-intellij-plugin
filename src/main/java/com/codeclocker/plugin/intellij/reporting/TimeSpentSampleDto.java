package com.codeclocker.plugin.intellij.reporting;

import java.util.List;

/**
 * DTO for time spent sample sent to the hub. Contains all activity data for a project in an hour
 * bucket.
 *
 * @param recordId unique identifier for this record (for idempotent sync), nullable for backward
 *     compatibility
 * @param hourKey hour bucket in format "yyyy-MM-dd-HH" in UTC timezone (e.g., "2025-12-28-10")
 * @param deltaSeconds seconds accumulated since last report (increment)
 * @param totalHourSeconds total seconds for this hour bucket (for verification)
 * @param additions VCS lines added (nullable for backward compatibility)
 * @param removals VCS lines removed (nullable for backward compatibility)
 * @param branchActivity list of branch activity records (nullable for backward compatibility)
 * @param commits list of commit records (nullable for backward compatibility)
 */
public record TimeSpentSampleDto(
    String recordId,
    String hourKey,
    long deltaSeconds,
    long totalHourSeconds,
    Long additions,
    Long removals,
    List<BranchActivityDto> branchActivity,
    List<CommitDto> commits) {

  /** Backward compatible constructor without new fields. */
  public TimeSpentSampleDto(
      String recordId, String hourKey, long deltaSeconds, long totalHourSeconds) {
    this(recordId, hourKey, deltaSeconds, totalHourSeconds, null, null, null, null);
  }

  /** DTO for branch activity within an hour. */
  public record BranchActivityDto(String branchName, long activeSeconds) {}

  /** DTO for commit record. */
  public record CommitDto(
      String hash,
      String message,
      String author,
      long timestamp,
      int changedFilesCount,
      String branch) {}
}
