package com.codeclocker.plugin.intellij.services;

import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

/**
 * Calculates total coding time by combining persisted data from LocalStateRepository with current
 * unsaved delta from accumulators. This is the single source of truth for displaying coding time.
 */
@Service(Service.Level.APP)
public final class CodingTimeCalculator {

  /**
   * Get total coded seconds for today across all projects. Combines persisted data from
   * LocalStateRepository with current unsaved delta from accumulators.
   *
   * @return total accumulated seconds today
   */
  public long getTodayTotalSeconds() {
    LocalStateRepository localState =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);

    long persistedSeconds = localState != null ? localState.getTodayTotalSeconds() : 0;
    long unsavedDelta = logger != null ? logger.getGlobalUnsavedDelta() : 0;

    return persistedSeconds + unsavedDelta;
  }

  /**
   * Get total coded seconds for today for a specific project. Combines persisted data from
   * LocalStateRepository with current unsaved delta from accumulator.
   *
   * @param projectName the project name
   * @return accumulated seconds for this project today
   */
  public long getTodayProjectSeconds(String projectName) {
    LocalStateRepository localState =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);

    long persistedSeconds = localState != null ? localState.getTodayProjectSeconds(projectName) : 0;
    long unsavedDelta = logger != null ? logger.getProjectUnsavedDelta(projectName) : 0;

    return persistedSeconds + unsavedDelta;
  }
}
