package com.codeclocker.plugin.intellij.standup;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.local.LocalActivityDataProvider;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service(Service.Level.APP)
public final class StandupDigestService {

  private static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
  private static final int MAX_COMMITS = 50;

  public StandupDigest compute(StandupPeriod period) {
    Map<String, Map<String, ProjectActivitySnapshot>> allData = getAllDataWithUnsaved();

    LocalDate today = LocalDate.now();
    LocalDate fromDate;
    LocalDate toDate;
    if (period == StandupPeriod.YESTERDAY) {
      fromDate = today.minusDays(1);
      toDate = today.minusDays(1);
    } else {
      fromDate = today.minusDays(period.getDays() - 1);
      toDate = today;
    }

    // Accumulators
    long totalSeconds = 0;
    long totalAdditions = 0;
    long totalRemovals = 0;
    Set<String> activeDateSet = new HashSet<>();
    Map<String, long[]> perProject = new LinkedHashMap<>(); // [seconds, additions, removals]
    Map<String, Long> perBranch = new LinkedHashMap<>();
    // projectName -> branchName -> list of commits
    Map<String, Map<String, List<CommitRecord>>> commitsByProjectBranch = new LinkedHashMap<>();
    Set<String> seenCommitHashes = new HashSet<>();
    // date -> [seconds, additions, removals]
    Map<String, long[]> perDay = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      if (!isInPeriod(hourKey, fromDate, toDate)) {
        continue;
      }

      String dateStr = extractDate(hourKey);
      long[] dayAcc = perDay.computeIfAbsent(dateStr, k -> new long[3]);

      for (Map.Entry<String, ProjectActivitySnapshot> projEntry : entry.getValue().entrySet()) {
        String projectName = projEntry.getKey();
        ProjectActivitySnapshot snapshot = projEntry.getValue();

        long seconds = snapshot.getCodedTimeSeconds();
        long additions = snapshot.getAdditions();
        long removals = snapshot.getRemovals();

        totalSeconds += seconds;
        totalAdditions += additions;
        totalRemovals += removals;

        if (seconds > 0 || additions > 0 || removals > 0) {
          activeDateSet.add(dateStr);
        }

        long[] projAcc = perProject.computeIfAbsent(projectName, k -> new long[3]);
        projAcc[0] += seconds;
        projAcc[1] += additions;
        projAcc[2] += removals;

        dayAcc[0] += seconds;
        dayAcc[1] += additions;
        dayAcc[2] += removals;

        for (var branchRecord : snapshot.getBranchActivity()) {
          String branchName = branchRecord.getBranchName();
          long branchSeconds = branchRecord.getActiveSeconds();
          if (branchName != null && !branchName.isEmpty() && branchSeconds > 0) {
            perBranch.merge(branchName, branchSeconds, Long::sum);
          }
        }

        for (CommitRecord commit : snapshot.getCommits()) {
          if (commit.getHash() != null && seenCommitHashes.add(commit.getHash())) {
            String branch = commit.getBranch() != null ? commit.getBranch() : "unknown";
            commitsByProjectBranch
                .computeIfAbsent(projectName, k -> new LinkedHashMap<>())
                .computeIfAbsent(branch, k -> new ArrayList<>())
                .add(commit);
          }
        }
      }
    }

    // Build project summaries sorted by time desc
    List<StandupDigest.ProjectSummary> projects = new ArrayList<>();
    for (Map.Entry<String, long[]> entry : perProject.entrySet()) {
      long[] acc = entry.getValue();
      if (acc[0] > 0 || acc[1] > 0 || acc[2] > 0) {
        projects.add(new StandupDigest.ProjectSummary(entry.getKey(), acc[0], acc[1], acc[2]));
      }
    }
    projects.sort((a, b) -> Long.compare(b.seconds(), a.seconds()));

    // Build branch summaries sorted by time desc
    List<StandupDigest.BranchSummary> branches = new ArrayList<>();
    for (Map.Entry<String, Long> entry : perBranch.entrySet()) {
      branches.add(new StandupDigest.BranchSummary(entry.getKey(), entry.getValue()));
    }
    branches.sort((a, b) -> Long.compare(b.seconds(), a.seconds()));

    // Build commit groups, capped at MAX_COMMITS total
    List<StandupDigest.ProjectCommitGroup> commitGroups = new ArrayList<>();
    int commitCount = 0;
    for (Map.Entry<String, Map<String, List<CommitRecord>>> projEntry :
        commitsByProjectBranch.entrySet()) {
      List<StandupDigest.BranchCommitGroup> branchGroups = new ArrayList<>();
      for (Map.Entry<String, List<CommitRecord>> branchEntry : projEntry.getValue().entrySet()) {
        List<CommitRecord> commits = branchEntry.getValue();
        commits.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        int remaining = MAX_COMMITS - commitCount;
        if (remaining <= 0) {
          break;
        }
        List<CommitRecord> capped =
            commits.size() > remaining ? commits.subList(0, remaining) : commits;
        branchGroups.add(new StandupDigest.BranchCommitGroup(branchEntry.getKey(), capped));
        commitCount += capped.size();
      }
      if (!branchGroups.isEmpty()) {
        commitGroups.add(new StandupDigest.ProjectCommitGroup(projEntry.getKey(), branchGroups));
      }
      if (commitCount >= MAX_COMMITS) {
        break;
      }
    }

    // Build daily breakdown (for 7-day mode), sorted most recent first
    List<StandupDigest.DailySummary> dailyBreakdown = new ArrayList<>();
    if (period != StandupPeriod.YESTERDAY) {
      for (Map.Entry<String, long[]> entry : perDay.entrySet()) {
        long[] acc = entry.getValue();
        if (acc[0] > 0 || acc[1] > 0 || acc[2] > 0) {
          LocalDate date = LocalDate.parse(entry.getKey());
          dailyBreakdown.add(new StandupDigest.DailySummary(date, acc[0], acc[1], acc[2]));
        }
      }
      dailyBreakdown.sort((a, b) -> b.date().compareTo(a.date()));
    }

    return new StandupDigest(
        period,
        fromDate,
        toDate,
        totalSeconds,
        totalAdditions,
        totalRemovals,
        activeDateSet.size(),
        projects,
        branches,
        commitGroups,
        dailyBreakdown);
  }

  private Map<String, Map<String, ProjectActivitySnapshot>> getAllDataWithUnsaved() {
    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    if (dataProvider == null) {
      return Collections.emptyMap();
    }

    Map<String, Map<String, ProjectActivitySnapshot>> allData =
        new LinkedHashMap<>(dataProvider.getAllDataInLocalTimezone());
    mergeUnsavedDeltas(allData);
    return allData;
  }

  private void mergeUnsavedDeltas(Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger == null) {
      return;
    }

    String currentHourKey = LocalDateTime.now().format(HOUR_KEY_FORMATTER);
    Map<String, ProjectActivitySnapshot> hourData =
        allData.computeIfAbsent(currentHourKey, k -> new LinkedHashMap<>());

    Set<String> projectNames = new HashSet<>();
    for (Map<String, ProjectActivitySnapshot> hourEntry : allData.values()) {
      projectNames.addAll(hourEntry.keySet());
    }

    for (String projectName : projectNames) {
      long unsavedDelta = logger.getProjectUnsavedDelta(projectName);
      if (unsavedDelta > 0) {
        ProjectActivitySnapshot existing = hourData.get(projectName);
        if (existing != null) {
          ProjectActivitySnapshot updated =
              new ProjectActivitySnapshot(
                  existing.getCodedTimeSeconds() + unsavedDelta,
                  existing.getAdditions(),
                  existing.getRemovals(),
                  existing.isReported());
          updated.setBranchActivity(existing.getBranchActivity());
          updated.setCommits(existing.getCommits());
          hourData.put(projectName, updated);
        } else {
          hourData.put(projectName, new ProjectActivitySnapshot(unsavedDelta, 0, 0, false));
        }
      }
    }
  }

  private boolean isInPeriod(String hourKey, LocalDate start, LocalDate end) {
    try {
      String dateStr = hourKey.substring(0, 10);
      LocalDate date = LocalDate.parse(dateStr);
      return !date.isBefore(start) && !date.isAfter(end);
    } catch (Exception e) {
      return false;
    }
  }

  private String extractDate(String hourKey) {
    if (hourKey != null && hourKey.length() >= 10) {
      return hourKey.substring(0, 10);
    }
    return hourKey;
  }
}
