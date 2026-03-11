package com.codeclocker.plugin.intellij.standup;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import java.time.LocalDate;
import java.util.List;

public record StandupDigest(
    StandupPeriod period,
    LocalDate fromDate,
    LocalDate toDate,
    long totalCodingSeconds,
    long totalAdditions,
    long totalRemovals,
    int activeDays,
    List<ProjectSummary> projects,
    List<BranchSummary> branches,
    List<ProjectCommitGroup> commitGroups,
    List<DailySummary> dailyBreakdown) {

  public record ProjectSummary(String projectName, long seconds, long additions, long removals) {}

  public record BranchSummary(String branchName, long seconds) {}

  public record ProjectCommitGroup(
      String projectName, List<BranchCommitGroup> branchCommitGroups) {}

  public record BranchCommitGroup(String branchName, List<CommitRecord> commits) {}

  public record DailySummary(LocalDate date, long seconds, long additions, long removals) {}
}
