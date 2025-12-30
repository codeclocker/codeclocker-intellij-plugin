package com.codeclocker.plugin.intellij.local;

import com.codeclocker.plugin.intellij.reporting.DataReportingTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository for persisting tracked time and VCS changes locally. Uses IntelliJ's
 * PersistentStateComponent for automatic XML serialization. Data is retained for max 2 weeks with
 * hour granularity.
 */
@State(name = "CodeClockerLocalState", storages = @Storage("codeclocker-local-state.xml"))
public class LocalStateRepository implements PersistentStateComponent<LocalTrackerState> {

  private static final Logger LOG = Logger.getInstance(LocalStateRepository.class);
  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private LocalTrackerState state = new LocalTrackerState();

  @Override
  public @Nullable LocalTrackerState getState() {
    ApplicationManager.getApplication()
        .getService(DataReportingTask.class)
        .saveToLocalStorageIfApiKeyIsEmpty();
    return state;
  }

  @Override
  public void loadState(@NotNull LocalTrackerState state) {
    this.state = state;
    // Cleanup old entries on load
    int removed = this.state.cleanupOldEntries();
    if (removed > 0) {
      LOG.info("Cleaned up " + removed + " old hour entries from local state");
    }
    LOG.debug("Loaded local tracker state with " + this.state.getTotalEntries() + " entries");
  }

  public void mergeProjectCurrentHour(String projectName, ProjectActivitySnapshot snapshot) {
    String currentHour = LocalDateTime.now().format(DATETIME_HOUR_FORMATTER);
    state.mergeProject(currentHour, projectName, snapshot);
    LOG.debug("Merged local state for project: " + projectName + " at hour: " + currentHour);
  }

  public Map<String, Map<String, ProjectActivitySnapshot>> getAllData() {
    return state.getHourlyActivity();
  }

  public Map<String, Map<String, ProjectActivitySnapshot>> getAllUnreportedData() {
    return state.getUnreportedData();
  }

  public void markAllDataAsReported() {
    LOG.debug("Marking all local state as reported");
    state.getHourlyActivity().values().stream()
        .flatMap(projects -> projects.values().stream())
        .forEach(snapshot -> snapshot.setReported(true));
  }

  public boolean hasUnreportedData() {
    return state.hasUnreportedEntries();
  }

  /** Cleanup entries older than 2 weeks. Called periodically. */
  public void rotate() {
    int removed = state.cleanupOldEntries();
    if (removed > 0) {
      LOG.info("Cleaned up " + removed + " old hour entries");
    }
  }

  /** Get total coded seconds for today across all projects from persisted data. */
  public long getTodayTotalSeconds() {
    String todayPrefix = LocalDate.now().toString();
    return state.getHourlyActivity().entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(todayPrefix))
        .flatMap(entry -> entry.getValue().values().stream())
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /** Get total coded seconds for today for a specific project from persisted data. */
  public long getTodayProjectSeconds(String projectName) {
    String todayPrefix = LocalDate.now().toString();
    return state.getHourlyActivity().entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(todayPrefix))
        .map(entry -> entry.getValue().get(projectName))
        .filter(Objects::nonNull)
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }
}
