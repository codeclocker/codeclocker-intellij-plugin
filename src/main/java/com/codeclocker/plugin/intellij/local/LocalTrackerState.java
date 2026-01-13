package com.codeclocker.plugin.intellij.local;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * State class for local persistence of tracked time and VCS changes. Structure: datetime
 * (YYYY-MM-DD-HH in UTC) -> project name -> activity snapshot. Data is retained for a maximum of 30
 * coding sessions (days with activity).
 */
public class LocalTrackerState {

  public static final String TIMEZONE_UTC = "UTC";

  /** Maximum number of coding sessions (days with activity) to retain locally. */
  public static final int MAX_SESSIONS = 30;

  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  /**
   * Timezone of hourKeys in hourlyActivity. null = legacy local timezone (needs migration), "UTC" =
   * migrated to UTC.
   */
  private String hourKeyTimezone;

  private Map<String, Map<String, ProjectActivitySnapshot>> hourlyActivity = new HashMap<>();

  public String getHourKeyTimezone() {
    return hourKeyTimezone;
  }

  public void setHourKeyTimezone(String hourKeyTimezone) {
    this.hourKeyTimezone = hourKeyTimezone;
  }

  public boolean needsMigrationToUtc() {
    return !TIMEZONE_UTC.equals(hourKeyTimezone);
  }

  public Map<String, Map<String, ProjectActivitySnapshot>> getHourlyActivity() {
    return hourlyActivity;
  }

  public void setHourlyActivity(Map<String, Map<String, ProjectActivitySnapshot>> hourlyActivity) {
    this.hourlyActivity = hourlyActivity;
  }

  public void mergeProject(
      String datetimeHour, String projectName, ProjectActivitySnapshot newSnapshot) {
    hourlyActivity.compute(
        datetimeHour,
        (dt, projects) -> {
          if (projects == null) {
            projects = new HashMap<>();
          }
          projects.merge(
              projectName,
              newSnapshot,
              (existing, incoming) -> {
                ProjectActivitySnapshot merged =
                    new ProjectActivitySnapshot(
                        existing.getCodedTimeSeconds() + incoming.getCodedTimeSeconds(),
                        existing.getAdditions() + incoming.getAdditions(),
                        existing.getRemovals() + incoming.getRemovals(),
                        existing.isReported());

                // Preserve recordId from existing entry (or use incoming's if existing has none)
                String recordId = existing.getRecordId();
                if (recordId == null || recordId.isEmpty()) {
                  recordId = incoming.getRecordId();
                }
                merged.setRecordId(recordId);

                // Merge branch activity (sum seconds per branch)
                Map<String, Long> branchMap = new HashMap<>();
                for (BranchActivityRecord b : existing.getBranchActivity()) {
                  branchMap.merge(b.getBranchName(), b.getActiveSeconds(), Long::sum);
                }
                for (BranchActivityRecord b : incoming.getBranchActivity()) {
                  branchMap.merge(b.getBranchName(), b.getActiveSeconds(), Long::sum);
                }
                List<BranchActivityRecord> mergedBranches = new ArrayList<>();
                for (Map.Entry<String, Long> e : branchMap.entrySet()) {
                  mergedBranches.add(new BranchActivityRecord(e.getKey(), e.getValue()));
                }
                merged.setBranchActivity(mergedBranches);

                // Merge commits (dedupe by hash)
                Set<String> existingHashes = new HashSet<>();
                for (CommitRecord c : existing.getCommits()) {
                  existingHashes.add(c.getHash());
                }
                List<CommitRecord> mergedCommits = new ArrayList<>(existing.getCommits());
                for (CommitRecord c : incoming.getCommits()) {
                  if (!existingHashes.contains(c.getHash())) {
                    mergedCommits.add(c);
                  }
                }
                merged.setCommits(mergedCommits);

                return merged;
              });
          return projects;
        });
  }

  /**
   * Removes entries beyond the maximum session limit. Keeps only the most recent MAX_SESSIONS days
   * (days with coding activity). Returns number of hour slots removed.
   */
  public int cleanupOldEntries() {
    // Group hourKeys by date to find unique sessions
    Map<String, List<String>> hourKeysByDate = new HashMap<>();
    for (String hourKey : hourlyActivity.keySet()) {
      if (hourKey != null && hourKey.length() >= 10) {
        String date = hourKey.substring(0, 10); // Extract yyyy-MM-dd
        hourKeysByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(hourKey);
      }
    }

    // If we have MAX_SESSIONS or fewer, no cleanup needed
    if (hourKeysByDate.size() <= MAX_SESSIONS) {
      return 0;
    }

    // Sort dates descending (newest first) and find dates to remove
    List<String> sortedDates = new ArrayList<>(hourKeysByDate.keySet());
    sortedDates.sort(Comparator.reverseOrder()); // Descending

    Set<String> datesToRemove = new HashSet<>();
    for (int i = MAX_SESSIONS; i < sortedDates.size(); i++) {
      datesToRemove.add(sortedDates.get(i));
    }

    // Remove all hourKeys for dates beyond the limit
    int removedCount = 0;
    Iterator<String> iterator = hourlyActivity.keySet().iterator();
    while (iterator.hasNext()) {
      String hourKey = iterator.next();
      if (hourKey != null && hourKey.length() >= 10) {
        String date = hourKey.substring(0, 10);
        if (datesToRemove.contains(date)) {
          iterator.remove();
          removedCount++;
        }
      }
    }

    return removedCount;
  }

  public boolean isEmpty() {
    return hourlyActivity.isEmpty();
  }

  public int getTotalEntries() {
    return hourlyActivity.values().stream().mapToInt(Map::size).sum();
  }

  public void clean() {
    hourlyActivity.clear();
  }

  public boolean hasUnreportedEntries() {
    return hourlyActivity.values().stream()
        .flatMap(m -> m.values().stream())
        .anyMatch(Predicate.not(ProjectActivitySnapshot::isReported));
  }

  public Map<String, Map<String, ProjectActivitySnapshot>> getUnreportedData() {
    Map<String, Map<String, ProjectActivitySnapshot>> unreported = new HashMap<>();
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry :
        hourlyActivity.entrySet()) {
      Map<String, ProjectActivitySnapshot> unreportedProjects = new HashMap<>();
      for (Map.Entry<String, ProjectActivitySnapshot> projectEntry :
          hourEntry.getValue().entrySet()) {
        if (!projectEntry.getValue().isReported()) {
          unreportedProjects.put(projectEntry.getKey(), projectEntry.getValue());
        }
      }
      if (!unreportedProjects.isEmpty()) {
        unreported.put(hourEntry.getKey(), unreportedProjects);
      }
    }
    return unreported;
  }

  /**
   * Ensures all snapshots have recordIds. Used during migration for existing entries that were
   * created before recordId was introduced.
   *
   * @return number of recordIds generated
   */
  public int ensureAllRecordIds() {
    int generated = 0;
    for (Map<String, ProjectActivitySnapshot> projects : hourlyActivity.values()) {
      for (ProjectActivitySnapshot snapshot : projects.values()) {
        if (snapshot.getRecordId() == null || snapshot.getRecordId().isEmpty()) {
          snapshot.ensureRecordId();
          generated++;
        }
      }
    }
    return generated;
  }
}
