package com.codeclocker.plugin.intellij.toolwindow;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import java.util.List;

/** Data row for branch activity table display. */
public record BranchActivityRow(
    String hourKey,
    String hourDisplay,
    String branchName,
    long seconds,
    String timeDisplay,
    List<CommitRecord> commits,
    String commitsDisplay) {}
