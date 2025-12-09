package com.codeclocker.plugin.intellij.local;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * State class for local persistence of tracked time and VCS changes. Structure: datetime
 * (YYYY-MM-DD-HH) -> project name -> activity snapshot. Data is retained for a maximum of 2 weeks.
 */
public class LocalTrackerState {

  private static final int RETENTION_DAYS = 14;
  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private Map<String, Map<String, ProjectActivitySnapshot>> hourlyActivity = new HashMap<>();

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
              (existing, incoming) ->
                  new ProjectActivitySnapshot(
                      existing.getCodedTimeSeconds() + incoming.getCodedTimeSeconds(),
                      existing.getAdditions() + incoming.getAdditions(),
                      existing.getRemovals() + incoming.getRemovals(),
                      existing.isReported()));
          return projects;
        });
  }

  /** Removes entries older than 2 weeks. Returns number of hour slots removed. */
  public int cleanupOldEntries() {
    LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(RETENTION_DAYS);
    int removedCount = 0;

    Iterator<String> iterator = hourlyActivity.keySet().iterator();
    while (iterator.hasNext()) {
      String datetimeStr = iterator.next();
      try {
        LocalDateTime entryDateTime = LocalDateTime.parse(datetimeStr, DATETIME_HOUR_FORMATTER);
        if (entryDateTime.isBefore(cutoffDateTime)) {
          iterator.remove();
          removedCount++;
        }
      } catch (Exception e) {
        // Invalid format, remove the entry
        iterator.remove();
        removedCount++;
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
}
