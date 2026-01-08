package com.codeclocker.plugin.intellij.local;

import com.codeclocker.plugin.intellij.reporting.DataReportingTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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

    // Migrate hourKeys from local timezone to UTC if needed
    if (this.state.needsMigrationToUtc()) {
      migrateHourKeysToUtc();
    }

    // Ensure all entries have recordIds (for backward compatibility with older data)
    int recordIdsGenerated = this.state.ensureAllRecordIds();
    if (recordIdsGenerated > 0) {
      LOG.info("Generated recordIds for " + recordIdsGenerated + " existing entries");
    }

    // Cleanup old entries on load
    int removed = this.state.cleanupOldEntries();
    if (removed > 0) {
      LOG.info("Cleaned up " + removed + " old hour entries from local state");
    }
    LOG.debug("Loaded local tracker state with " + this.state.getTotalEntries() + " entries");
  }

  /**
   * Migrates all hourKeys from local timezone to UTC. This is a one-time migration for existing
   * data that was stored using the host's timezone.
   */
  private void migrateHourKeysToUtc() {
    Map<String, Map<String, ProjectActivitySnapshot>> oldData = state.getHourlyActivity();
    if (oldData.isEmpty()) {
      state.setHourKeyTimezone(LocalTrackerState.TIMEZONE_UTC);
      LOG.info("No data to migrate, setting timezone to UTC");
      return;
    }

    LOG.info("Migrating " + oldData.size() + " hour entries from local timezone to UTC");

    Map<String, Map<String, ProjectActivitySnapshot>> migratedData = new HashMap<>();
    ZoneId localZone = ZoneId.systemDefault();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : oldData.entrySet()) {
      String localHourKey = entry.getKey();
      String utcHourKey = convertLocalHourKeyToUtc(localHourKey, localZone);

      // Merge into migrated data (in case of collision, though unlikely)
      migratedData.compute(
          utcHourKey,
          (key, existingProjects) -> {
            if (existingProjects == null) {
              return new HashMap<>(entry.getValue());
            }
            // Merge projects if collision occurs
            for (Map.Entry<String, ProjectActivitySnapshot> projectEntry :
                entry.getValue().entrySet()) {
              existingProjects.merge(
                  projectEntry.getKey(),
                  projectEntry.getValue(),
                  (existing, incoming) ->
                      new ProjectActivitySnapshot(
                          existing.getCodedTimeSeconds() + incoming.getCodedTimeSeconds(),
                          existing.getAdditions() + incoming.getAdditions(),
                          existing.getRemovals() + incoming.getRemovals(),
                          existing.isReported() && incoming.isReported()));
            }
            return existingProjects;
          });
    }

    state.setHourlyActivity(migratedData);
    state.setHourKeyTimezone(LocalTrackerState.TIMEZONE_UTC);
    LOG.info("Migration complete. Converted " + oldData.size() + " entries to UTC");
  }

  private String convertLocalHourKeyToUtc(String localHourKey, ZoneId localZone) {
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(localHourKey, DATETIME_HOUR_FORMATTER);
      ZonedDateTime utcDateTime =
          localDateTime.atZone(localZone).withZoneSameInstant(ZoneId.of("UTC"));
      return utcDateTime.format(DATETIME_HOUR_FORMATTER);
    } catch (Exception e) {
      LOG.warn("Failed to convert hourKey to UTC: " + localHourKey, e);
      return localHourKey;
    }
  }

  public void mergeProjectCurrentHour(String projectName, ProjectActivitySnapshot snapshot) {
    // Ensure snapshot has a recordId for idempotent sync
    snapshot.ensureRecordId();
    String currentUtcHour = ZonedDateTime.now(ZoneId.of("UTC")).format(DATETIME_HOUR_FORMATTER);
    state.mergeProject(currentUtcHour, projectName, snapshot);
    LOG.debug("Merged local state for project: " + projectName + " at UTC hour: " + currentUtcHour);
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
}
