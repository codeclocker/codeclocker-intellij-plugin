package com.codeclocker.plugin.intellij.standup;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public final class StandupDigestFormatter {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d");
  private static final DateTimeFormatter DAY_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEE MMM d");

  private StandupDigestFormatter() {}

  public static String format(StandupDigest digest) {
    if (digest.totalCodingSeconds() == 0
        && digest.totalAdditions() == 0
        && digest.totalRemovals() == 0
        && digest.commitGroups().isEmpty()) {
      return emptyMessage(digest);
    }

    StringBuilder sb = new StringBuilder();
    appendHeader(sb, digest);
    appendTotals(sb, digest);

    if (digest.period() != StandupPeriod.YESTERDAY && !digest.dailyBreakdown().isEmpty()) {
      appendDailyBreakdown(sb, digest.dailyBreakdown());
    }

    if (!digest.projects().isEmpty()) {
      appendProjects(sb, digest.projects());
    }

    if (!digest.branches().isEmpty()) {
      appendBranches(sb, digest.branches());
    }

    if (!digest.commitGroups().isEmpty()) {
      appendCommits(sb, digest.commitGroups());
    }

    return sb.toString().stripTrailing();
  }

  private static String emptyMessage(StandupDigest digest) {
    if (digest.period() == StandupPeriod.YESTERDAY) {
      return "No coding activity recorded for yesterday.";
    }
    return String.format(
        "No coding activity recorded for the %s.", digest.period().getLabel().toLowerCase());
  }

  private static void appendHeader(StringBuilder sb, StandupDigest digest) {
    if (digest.period() == StandupPeriod.YESTERDAY) {
      String dayName =
          digest.fromDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
      sb.append(
          String.format("Yesterday (%s, %s)%n", dayName, digest.fromDate().format(DATE_FORMATTER)));
    } else {
      sb.append(
          String.format(
              "%s (%s \u2014 %s)%n",
              digest.period().getLabel(),
              digest.fromDate().format(DATE_FORMATTER),
              digest.toDate().format(DATE_FORMATTER)));
    }
  }

  private static void appendTotals(StringBuilder sb, StandupDigest digest) {
    sb.append(String.format("%nTotal: %s", formatTime(digest.totalCodingSeconds())));
    if (digest.totalAdditions() > 0 || digest.totalRemovals() > 0) {
      sb.append(
          String.format(" | +%d / -%d lines", digest.totalAdditions(), digest.totalRemovals()));
    }
    if (digest.period() != StandupPeriod.YESTERDAY) {
      sb.append(
          String.format(
              " | %d active day%s", digest.activeDays(), digest.activeDays() == 1 ? "" : "s"));
    }
    sb.append(String.format("%n"));
  }

  private static void appendDailyBreakdown(
      StringBuilder sb, List<StandupDigest.DailySummary> dailyBreakdown) {
    sb.append(String.format("%nDaily:%n"));
    for (StandupDigest.DailySummary day : dailyBreakdown) {
      String dayLabel = day.date().format(DAY_DATE_FORMATTER);
      sb.append(String.format("  %-12s %8s", dayLabel, formatTime(day.seconds())));
      if (day.additions() > 0 || day.removals() > 0) {
        sb.append(String.format("  (+%d / -%d)", day.additions(), day.removals()));
      }
      sb.append(String.format("%n"));
    }
  }

  private static void appendProjects(
      StringBuilder sb, List<StandupDigest.ProjectSummary> projects) {
    sb.append(String.format("%nProjects:%n"));
    int maxNameLen = 0;
    for (StandupDigest.ProjectSummary p : projects) {
      maxNameLen = Math.max(maxNameLen, p.projectName().length());
    }
    String fmt = "  %-" + (maxNameLen + 2) + "s %8s";
    for (StandupDigest.ProjectSummary p : projects) {
      sb.append(String.format(fmt, p.projectName(), formatTime(p.seconds())));
      if (p.additions() > 0 || p.removals() > 0) {
        sb.append(String.format("  (+%d / -%d)", p.additions(), p.removals()));
      }
      sb.append(String.format("%n"));
    }
  }

  private static void appendBranches(StringBuilder sb, List<StandupDigest.BranchSummary> branches) {
    sb.append(String.format("%nBranches:%n"));
    int maxNameLen = 0;
    for (StandupDigest.BranchSummary b : branches) {
      maxNameLen = Math.max(maxNameLen, b.branchName().length());
    }
    String fmt = "  %-" + (maxNameLen + 2) + "s %8s%n";
    for (StandupDigest.BranchSummary b : branches) {
      sb.append(String.format(fmt, b.branchName(), formatTime(b.seconds())));
    }
  }

  private static void appendCommits(
      StringBuilder sb, List<StandupDigest.ProjectCommitGroup> commitGroups) {
    int totalCommits = 0;
    for (StandupDigest.ProjectCommitGroup pg : commitGroups) {
      for (StandupDigest.BranchCommitGroup bg : pg.branchCommitGroups()) {
        totalCommits += bg.commits().size();
      }
    }

    sb.append(String.format("%nCommits (%d):%n", totalCommits));
    for (StandupDigest.ProjectCommitGroup pg : commitGroups) {
      for (StandupDigest.BranchCommitGroup bg : pg.branchCommitGroups()) {
        sb.append(String.format("  [%s] %s%n", pg.projectName(), bg.branchName()));
        for (CommitRecord commit : bg.commits()) {
          String message = firstLine(commit.getMessage());
          sb.append(String.format("    %s  %s%n", commit.getHash(), message));
        }
      }
    }
  }

  private static String formatTime(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    if (hours > 0) {
      return String.format("%dh %dm", hours, minutes);
    }
    return String.format("%dm", minutes);
  }

  private static String firstLine(String text) {
    if (text == null) {
      return "";
    }
    return text.lines().findFirst().orElse("");
  }
}
