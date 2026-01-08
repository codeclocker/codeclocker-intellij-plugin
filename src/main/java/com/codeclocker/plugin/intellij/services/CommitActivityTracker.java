package com.codeclocker.plugin.intellij.services;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks commit activity per project and hour bucket. Stores commit records that can be drained
 * during periodic data flush.
 */
@Service(Service.Level.APP)
public final class CommitActivityTracker {

  private static final Logger LOG = Logger.getInstance(CommitActivityTracker.class);
  private static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  /** Commits by project and hour: project -> hour -> commits list. */
  private final Map<String, Map<String, List<CommitRecord>>> commitsByProject =
      new ConcurrentHashMap<>();

  /**
   * Record a new commit for a project.
   *
   * @param projectName the project name
   * @param commit the commit record
   */
  public void recordCommit(String projectName, CommitRecord commit) {
    String hourKey = LocalDateTime.now().format(HOUR_KEY_FORMATTER);

    commitsByProject
        .computeIfAbsent(projectName, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(hourKey, k -> new CopyOnWriteArrayList<>())
        .add(commit);

    LOG.info(
        "Recorded commit "
            + commit.getHash()
            + " for project "
            + projectName
            + " in hour "
            + hourKey);
  }

  /**
   * Drain and return commits for a project/hour, clearing the data.
   *
   * @param projectName the project name
   * @param hourKey the hour key
   * @return list of commits, or empty list if no data
   */
  public List<CommitRecord> drainCommits(String projectName, String hourKey) {
    Map<String, List<CommitRecord>> projectCommits = commitsByProject.get(projectName);
    if (projectCommits == null) {
      return new ArrayList<>();
    }

    List<CommitRecord> hourCommits = projectCommits.remove(hourKey);
    return hourCommits != null ? new ArrayList<>(hourCommits) : new ArrayList<>();
  }

  /**
   * Drain all commits for a project across all hours.
   *
   * @param projectName the project name
   * @return map of hourKey to commits list
   */
  public Map<String, List<CommitRecord>> drainAllCommits(String projectName) {
    Map<String, List<CommitRecord>> projectCommits = commitsByProject.remove(projectName);
    if (projectCommits == null) {
      return new HashMap<>();
    }
    return new HashMap<>(projectCommits);
  }
}
