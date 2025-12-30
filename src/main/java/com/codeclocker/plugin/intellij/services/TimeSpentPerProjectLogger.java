package com.codeclocker.plugin.intellij.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks coding time per project using accumulated seconds approach. Replaces the previous
 * stopwatch-based implementation with a simpler model that:
 *
 * <ul>
 *   <li>Tracks time per project in hour buckets
 *   <li>Supports delta-based reporting to the hub
 *   <li>Handles hour boundaries and midnight resets
 * </ul>
 */
public class TimeSpentPerProjectLogger {

  private static final Logger LOG = Logger.getInstance(TimeSpentPerProjectLogger.class);

  /** Per-project time accumulators - the single source of truth for all time data. */
  private final Map<String, ProjectTimeAccumulator> accumulatorsByProject =
      new ConcurrentHashMap<>();

  /** Currently active project. */
  private final AtomicReference<String> currentProjectName = new AtomicReference<>();

  /** Track last date for midnight reset. */
  private volatile LocalDate lastDate = LocalDate.now();

  /** Hour transitions that occurred and need to be reported. */
  private final List<HourTransitionRecord> pendingHourTransitions =
      Collections.synchronizedList(new ArrayList<>());

  /**
   * Called when user activity is detected in a project. Calculates elapsed time since last activity
   * and adds to accumulator.
   */
  public void log(Project project) {
    if (project == null || project.isDisposed()) {
      return;
    }

    String projectName = project.getName();
    long now = System.currentTimeMillis();

    deactivatePrevProject(projectName, now);

    ProjectTimeAccumulator accumulator =
        accumulatorsByProject.computeIfAbsent(projectName, k -> new ProjectTimeAccumulator());

    checkHourBoundary(accumulator, projectName);

    accumulator.calculateAndAddElapsed(now);
    accumulator.activate(now);
  }

  private void checkHourBoundary(ProjectTimeAccumulator accumulator, String projectName) {
    ProjectTimeAccumulator.HourTransition transition = accumulator.checkAndHandleHourBoundary();
    if (transition != null && transition.hasUnreportedSeconds()) {
      synchronized (pendingHourTransitions) {
        pendingHourTransitions.add(new HourTransitionRecord(projectName, transition));
      }
      LOG.debug(
          "Hour transition for {}: {} with {} unreported seconds",
          projectName,
          transition.hourKey(),
          transition.getDelta());
    }
  }

  private void deactivatePrevProject(String currentProjectName, long now) {
    String prevProjectName = this.currentProjectName.getAndSet(currentProjectName);
    if (prevProjectName != null && !Objects.equals(prevProjectName, currentProjectName)) {
      deactivateProject(prevProjectName, now);
    }
  }

  public void pauseDueToInactivity() {
    String projectName = currentProjectName.get();
    if (projectName != null) {
      deactivateProject(projectName, System.currentTimeMillis());
    }
  }

  public void closeProject(Project project) {
    if (project == null) {
      return;
    }
    deactivateProject(project.getName(), System.currentTimeMillis());
    currentProjectName.updateAndGet(
        current -> current != null && Objects.equals(current, project.getName()) ? null : current);
  }

  private void deactivateProject(String projectName, long now) {
    ProjectTimeAccumulator accumulator = accumulatorsByProject.get(projectName);
    if (accumulator != null) {
      accumulator.calculateAndAddElapsed(now);
      accumulator.deactivate();
    }
  }

  /**
   * Get deltas for all projects for reporting. Does NOT clear data - just marks what was reported.
   *
   * @return map of project name to delta info
   */
  public Map<String, ProjectTimeDelta> getProjectDeltas() {
    Map<String, ProjectTimeDelta> deltas = new HashMap<>();

    // First, add any pending hour transitions
    synchronized (pendingHourTransitions) {
      for (HourTransitionRecord record : pendingHourTransitions) {
        ProjectTimeAccumulator.HourTransition t = record.transition;
        if (t.getDelta() > 0) {
          deltas.merge(
              record.projectName,
              new ProjectTimeDelta(t.hourKey(), t.getDelta(), t.accumulatedSeconds()),
              (existing, incoming) ->
                  new ProjectTimeDelta(
                      incoming.hourKey(),
                      existing.deltaSeconds() + incoming.deltaSeconds(),
                      incoming.totalHourSeconds()));
        }
      }
      pendingHourTransitions.clear();
    }

    // Then add current hour deltas
    for (Map.Entry<String, ProjectTimeAccumulator> entry : accumulatorsByProject.entrySet()) {
      String projectName = entry.getKey();
      ProjectTimeAccumulator acc = entry.getValue();

      long delta = acc.getUnreportedDeltaAndMarkReported();
      if (delta > 0) {
        // Merge with any existing delta for this project (from hour transitions)
        deltas.merge(
            projectName,
            new ProjectTimeDelta(acc.getHourKey(), delta, acc.getAccumulatedSeconds()),
            (existing, incoming) ->
                new ProjectTimeDelta(
                    incoming.hourKey(),
                    existing.deltaSeconds() + incoming.deltaSeconds(),
                    incoming.totalHourSeconds()));
      }
    }

    return deltas;
  }

  /**
   * Get total seconds for today across all projects. Delegates to CodingTimeCalculator.
   *
   * @return total accumulated seconds today
   */
  public long getGlobalAccumulatedToday() {
    CodingTimeCalculator calculator =
        ApplicationManager.getApplication().getService(CodingTimeCalculator.class);
    return calculator != null ? calculator.getTodayTotalSeconds() : getGlobalUnsavedDelta();
  }

  /**
   * Get project-specific accumulated seconds for today. Delegates to CodingTimeCalculator.
   *
   * @param projectName the project name
   * @return accumulated seconds for this project today
   */
  public long getProjectAccumulatedToday(String projectName) {
    CodingTimeCalculator calculator =
        ApplicationManager.getApplication().getService(CodingTimeCalculator.class);
    return calculator != null
        ? calculator.getTodayProjectSeconds(projectName)
        : getProjectUnsavedDelta(projectName);
  }

  /** Get the current unsaved delta across all projects (time accumulated since last flush). */
  public long getGlobalUnsavedDelta() {
    String todayPrefix = LocalDate.now().toString();
    return accumulatorsByProject.values().stream()
        .filter(acc -> acc.getHourKey().startsWith(todayPrefix))
        .mapToLong(ProjectTimeAccumulator::getUnsavedDelta)
        .sum();
  }

  /** Get the current unsaved delta for a specific project (time accumulated since last flush). */
  public long getProjectUnsavedDelta(String projectName) {
    ProjectTimeAccumulator acc = accumulatorsByProject.get(projectName);
    if (acc != null) {
      String todayPrefix = LocalDate.now().toString();
      if (acc.getHourKey().startsWith(todayPrefix)) {
        return acc.getUnsavedDelta();
      }
    }
    return 0;
  }

  /**
   * Initialize accumulators from local state on startup.
   *
   * @param projectSecondsToday map of project name to seconds accumulated today
   */
  public void initializeFromLocalState(Map<String, Long> projectSecondsToday) {
    for (Map.Entry<String, Long> entry : projectSecondsToday.entrySet()) {
      String projectName = entry.getKey();
      long seconds = entry.getValue();

      ProjectTimeAccumulator acc = new ProjectTimeAccumulator();
      acc.setAccumulatedSeconds(seconds);
      // Mark as already reported since this is loaded from persistent state
      acc.getUnreportedDeltaAndMarkReported();
      accumulatorsByProject.put(projectName, acc);
    }

    LOG.info(
        "Initialized time tracking from local state: "
            + projectSecondsToday.size()
            + " projects, "
            + getGlobalAccumulatedToday()
            + "s total");
  }

  /**
   * Mark new day (called at midnight by widget service). Does not clear accumulators - getters
   * filter by today's date instead to avoid data loss.
   */
  public void resetForNewDay() {
    LOG.info("New day detected, updating date marker");
    lastDate = LocalDate.now();
  }

  /** Check if midnight has passed (for external callers like widget service). */
  public boolean hasMidnightPassed() {
    return !LocalDate.now().equals(lastDate);
  }

  /** Delta information for a project to be reported to the hub. */
  public record ProjectTimeDelta(String hourKey, long deltaSeconds, long totalHourSeconds) {}

  /** Record of an hour transition for a specific project. */
  private record HourTransitionRecord(
      String projectName, ProjectTimeAccumulator.HourTransition transition) {}
}
