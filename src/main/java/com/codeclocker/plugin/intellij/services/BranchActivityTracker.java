package com.codeclocker.plugin.intellij.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks git branch activity per project. Records which branch is active and accumulates time spent
 * on each branch per hour bucket.
 */
@Service(Service.Level.APP)
public final class BranchActivityTracker {

  private static final Logger LOG = Logger.getInstance(BranchActivityTracker.class);
  private static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
  private static final long MILLIS_PER_SECOND = 1000L;

  /** Current branch per project. */
  private final Map<String, String> currentBranchByProject = new ConcurrentHashMap<>();

  /** Branch activity: project -> hour -> branch -> seconds. */
  private final Map<String, Map<String, Map<String, Long>>> branchActivityByProject =
      new ConcurrentHashMap<>();

  /** Last activity tick timestamp per project for calculating elapsed time. */
  private final Map<String, Long> lastTickTimestampByProject = new ConcurrentHashMap<>();

  /** Whether tracking is active per project. */
  private final Map<String, Boolean> activeByProject = new ConcurrentHashMap<>();

  /**
   * Called when branch changes in a project.
   *
   * @param projectName the project name
   * @param newBranch the new branch name (or "detached" if in detached HEAD state)
   */
  public void onBranchChange(String projectName, String newBranch) {
    String previousBranch = currentBranchByProject.put(projectName, newBranch);
    if (previousBranch != null && !previousBranch.equals(newBranch)) {
      LOG.info("Branch changed in " + projectName + ": " + previousBranch + " -> " + newBranch);
    }
  }

  /**
   * Records a tick of activity on the current branch. Called when TimeSpentPerProjectLogger.log()
   * is invoked. Calculates elapsed time since last tick.
   *
   * @param projectName the project name
   */
  public void recordActivityTick(String projectName) {
    String branch = currentBranchByProject.get(projectName);
    if (branch == null) {
      return;
    }

    long now = System.currentTimeMillis();
    Long lastTick = lastTickTimestampByProject.get(projectName);
    Boolean wasActive = activeByProject.get(projectName);

    // Update tracking state
    lastTickTimestampByProject.put(projectName, now);
    activeByProject.put(projectName, true);

    // Calculate elapsed if we were actively tracking
    if (lastTick != null && Boolean.TRUE.equals(wasActive)) {
      long elapsedMillis = now - lastTick;
      long elapsedSeconds = Math.round((float) elapsedMillis / MILLIS_PER_SECOND);

      if (elapsedSeconds > 0 && elapsedSeconds < 300) { // Cap at 5 minutes to avoid huge gaps
        String hourKey = LocalDateTime.now().format(HOUR_KEY_FORMATTER);
        branchActivityByProject
            .computeIfAbsent(projectName, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(hourKey, k -> new ConcurrentHashMap<>())
            .merge(branch, elapsedSeconds, Long::sum);
      }
    }
  }

  /** Mark project as inactive (due to inactivity timeout). */
  public void pauseProject(String projectName) {
    activeByProject.put(projectName, false);
  }

  /**
   * Get current branch for a project.
   *
   * @param projectName the project name
   * @return the current branch name, or null if not tracked
   */
  public String getCurrentBranch(String projectName) {
    return currentBranchByProject.get(projectName);
  }

  /**
   * Drain and return branch activity for a project/hour, clearing the data.
   *
   * @param projectName the project name
   * @param hourKey the hour key
   * @return map of branch name to seconds, or empty map if no data
   */
  public Map<String, Long> drainBranchActivity(String projectName, String hourKey) {
    Map<String, Map<String, Long>> projectActivity = branchActivityByProject.get(projectName);
    if (projectActivity == null) {
      return new HashMap<>();
    }

    Map<String, Long> hourActivity = projectActivity.remove(hourKey);
    return hourActivity != null ? new HashMap<>(hourActivity) : new HashMap<>();
  }

  /**
   * Drain all branch activity for a project across all hours.
   *
   * @param projectName the project name
   * @return map of hourKey to (branch -> seconds)
   */
  public Map<String, Map<String, Long>> drainAllBranchActivity(String projectName) {
    Map<String, Map<String, Long>> projectActivity = branchActivityByProject.remove(projectName);
    if (projectActivity == null) {
      return new HashMap<>();
    }
    return new HashMap<>(projectActivity);
  }

  /**
   * Initialize branch tracking for a project by reading current branch from git.
   *
   * @param project the IntelliJ project
   */
  public void initializeFromGit(Project project) {
    if (project == null || project.isDisposed()) {
      return;
    }

    try {
      GitRepositoryManager gitManager = GitRepositoryManager.getInstance(project);
      if (gitManager == null) {
        return;
      }

      for (GitRepository repo : gitManager.getRepositories()) {
        String branchName = getBranchName(repo);
        currentBranchByProject.put(project.getName(), branchName);
        LOG.info("Initialized branch tracking for " + project.getName() + ": " + branchName);
        break; // Use first repository for now
      }
    } catch (Exception e) {
      LOG.warn("Failed to initialize branch tracking for " + project.getName(), e);
    }
  }

  private String getBranchName(GitRepository repo) {
    if (repo.getCurrentBranch() != null) {
      return repo.getCurrentBranch().getName();
    }
    // Detached HEAD state - show short hash
    String revision = repo.getCurrentRevision();
    if (revision != null && revision.length() > 7) {
      return "detached:" + revision.substring(0, 7);
    }
    return "detached";
  }
}
