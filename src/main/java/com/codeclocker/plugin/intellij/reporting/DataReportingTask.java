package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.Config;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.ChangesSample;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectSample;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.subscription.CheckSubscriptionStateHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;

public final class DataReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(CheckSubscriptionStateHttpClient.class);

  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private final int flushToServerFrequencySeconds;
  private final TimeSpentPerProjectLogger timeSpentPerProjectLogger;
  private final ChangesActivityTracker changesActivityTracker;
  private final ActivitySampleHttpClient activitySampleHttpClient;
  private final LocalStateRepository localStateRepository;
  private final Queue<String> unpublishedTimeSpentSamples = new ArrayDeque<>();
  private final Queue<String> unpublishedChangesSamples = new ArrayDeque<>();

  private ScheduledFuture<?> task;

  public DataReportingTask() {
    this.changesActivityTracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    this.timeSpentPerProjectLogger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    this.activitySampleHttpClient =
        ApplicationManager.getApplication().getService(ActivitySampleHttpClient.class);
    ConfigProvider configProvider =
        ApplicationManager.getApplication().getService(ConfigProvider.class);
    this.flushToServerFrequencySeconds = configProvider.getActivityDataFlushFrequencySeconds();
    this.localStateRepository =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);
  }

  public void schedule() {
    if (task != null && !task.isCancelled()) {
      return;
    }

    task =
        EXECUTOR.scheduleWithFixedDelay(
            this::flushActivityData,
            flushToServerFrequencySeconds,
            flushToServerFrequencySeconds,
            SECONDS);
  }

  public void flushActivityData() {
    try {
      String apiKey = ApiKeyLifecycle.getActiveApiKey();

      // Cleanup old local data periodically
      localStateRepository.rotate();

      // Always save to local storage first
      saveToLocalStorage();

      // If API key is available, try to sync to server
      if (!isBlank(apiKey)) {
        sendActivitySampleToServer(apiKey);
      }
    } catch (Exception ex) {
      LOG.debug("Error flushing activity data: {}", ex.getMessage());
    }
  }

  public void saveToLocalStorageIfApiKeyIsEmpty() { // todo: store in any case
    String apiKey = ApiKeyLifecycle.getActiveApiKey();
    if (isBlank(apiKey)) {
      saveToLocalStorage();
    }
  }

  public void saveToLocalStorage() {
    Map<String, TimeSpentPerProjectSample> timeSamples = timeSpentPerProjectLogger.drain();
    Map<String, Map<String, ChangesSample>> changesSamples = changesActivityTracker.drain();

    if (timeSamples.isEmpty() && changesSamples.isEmpty()) {
      LOG.debug("No activity data to save locally");
      return;
    }

    // Aggregate VCS changes per project
    Map<String, Long> projectAdditions = new HashMap<>();
    Map<String, Long> projectRemovals = new HashMap<>();

    for (Entry<String, Map<String, ChangesSample>> projectEntry : changesSamples.entrySet()) {
      String projectName = projectEntry.getKey();
      long totalAdditions = 0;
      long totalRemovals = 0;

      for (ChangesSample fileSample : projectEntry.getValue().values()) {
        totalAdditions += fileSample.additions().get();
        totalRemovals += fileSample.removals().get();
      }

      projectAdditions.put(projectName, totalAdditions);
      projectRemovals.put(projectName, totalRemovals);
    }

    // Save time spent per project
    for (Entry<String, TimeSpentPerProjectSample> entry : timeSamples.entrySet()) {
      String projectName = entry.getKey();
      long timeSeconds = entry.getValue().timeSpent().getSeconds();
      long additions = projectAdditions.getOrDefault(projectName, 0L);
      long removals = projectRemovals.getOrDefault(projectName, 0L);

      ProjectActivitySnapshot snapshot =
          new ProjectActivitySnapshot(timeSeconds, additions, removals, false);
      localStateRepository.mergeProjectCurrentHour(projectName, snapshot);
    }

    // Save VCS changes for projects without time entries
    for (String projectName : projectAdditions.keySet()) {
      if (!timeSamples.containsKey(projectName)) {
        long additions = projectAdditions.get(projectName);
        long removals = projectRemovals.get(projectName);

        ProjectActivitySnapshot snapshot =
            new ProjectActivitySnapshot(0, additions, removals, false);
        localStateRepository.mergeProjectCurrentHour(projectName, snapshot);
      }
    }

    LOG.debug("Saved activity data to local storage for " + timeSamples.size() + " projects");
  }

  private void sendActivitySampleToServer(String apiKey) {
    // Validate timers before flushing to detect inconsistencies
    validateTimersBeforeFlush();

    // First, sync any locally stored data to the server
    syncLocalDataToServer(apiKey);

    SentStatus unpublishedSamplesPublishStatus = publishUnpublishedSamples(apiKey);
    if (unpublishedSamplesPublishStatus == ERROR) {
      LOG.debug("Failed to publish unpublished samples");
      return;
    }

    publishTimeSpentSample(apiKey);
    publishChangesSample(apiKey);
  }

  public void syncLocalDataToServer(String apiKey) {
    if (!localStateRepository.hasUnreportedData()) {
      return;
    }

    LOG.info("Found locally stored data, syncing to server...");

    // Get data without clearing - only clear after successful send
    Map<String, Map<String, ProjectActivitySnapshot>> localData =
        localStateRepository.getAllUnreportedData();
    if (localData.isEmpty()) {
      return;
    }

    // Convert local data to DTOs and send
    // Group by project across all dates for time spent
    Map<String, TimeSpentSampleDto> timeSpentByProject = new HashMap<>();
    // For changes, we send per-project aggregated data
    Map<String, Map<String, ChangesSampleDto>> changesByProject = new HashMap<>();

    for (Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : localData.entrySet()) {
      String datetimeHourStr = hourEntry.getKey();
      long samplingStartedAt = datetimeHourToTimestamp(datetimeHourStr);

      for (Entry<String, ProjectActivitySnapshot> projectEntry : hourEntry.getValue().entrySet()) {
        String projectName = projectEntry.getKey();
        ProjectActivitySnapshot snapshot = projectEntry.getValue();

        // Aggregate time spent per project
        if (snapshot.getCodedTimeSeconds() > 0) {
          timeSpentByProject.merge(
              projectName,
              new TimeSpentSampleDto(samplingStartedAt, snapshot.getCodedTimeSeconds()),
              (existing, incoming) ->
                  new TimeSpentSampleDto(
                      Math.min(existing.samplingStartedAt(), incoming.samplingStartedAt()),
                      existing.timeSpentSeconds() + incoming.timeSpentSeconds()));
        }

        // Aggregate VCS changes per project
        if (snapshot.getAdditions() > 0 || snapshot.getRemovals() > 0) {
          String syntheticFileName = "local-sync-" + datetimeHourStr;
          ChangesSampleDto changesDto =
              new ChangesSampleDto(
                  samplingStartedAt,
                  snapshot.getAdditions(),
                  snapshot.getRemovals(),
                  Collections.emptyMap());

          changesByProject
              .computeIfAbsent(projectName, k -> new HashMap<>())
              .put(syntheticFileName, changesDto);
        }
      }
    }

    // Send time spent data
    boolean timeSyncSuccess = true;
    if (!timeSpentByProject.isEmpty()) {
      String timeJson = toJson(timeSpentByProject);
      SentStatus status = activitySampleHttpClient.sendTimeSpentSample(apiKey, timeJson);
      if (status == ERROR) {
        LOG.warn("Failed to sync local time spent data to server, will retry later");
        timeSyncSuccess = false;
      } else {
        LOG.info("Synced local time spent data for " + timeSpentByProject.size() + " projects");
      }
    }

    // Send changes data
    boolean changesSyncSuccess = true;
    if (!changesByProject.isEmpty()) {
      String changesJson = toJson(changesByProject);
      SentStatus status = activitySampleHttpClient.sendChangesSample(apiKey, changesJson);
      if (status == ERROR) {
        LOG.warn("Failed to sync local VCS changes data to server, will retry later");
        changesSyncSuccess = false;
      } else {
        LOG.info("Synced local VCS changes data for " + changesByProject.size() + " projects");
      }
    }

    // Only clear local data if both syncs succeeded
    if (timeSyncSuccess && changesSyncSuccess) {
      localStateRepository.markAllDataAsReported();
      LOG.info("Cleared local data after successful sync");
    }
  }

  private static long datetimeHourToTimestamp(String datetimeHourStr) {
    try {
      LocalDateTime dateTime = LocalDateTime.parse(datetimeHourStr, DATETIME_HOUR_FORMATTER);
      return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    } catch (Exception e) {
      // Fallback to current time if parsing fails
      return System.currentTimeMillis();
    }
  }

  private void validateTimersBeforeFlush() {
    if (!Config.isValidateTimersEnabled()) {
      return;
    }

    try {
      TimeSpentPerProjectLogger.ValidationResult result =
          timeSpentPerProjectLogger.validateTimers();

      if (!result.isValid()) {
        LOG.warn("Timer validation failed before flush: " + result.getSummary());
      } else {
        LOG.debug("Timer validation passed: " + result.getSummary());
      }
    } catch (Exception ex) {
      LOG.warn("Error during timer validation", ex);
    }
  }

  private void publishTimeSpentSample(String apiKey) {
    Map<String, TimeSpentPerProjectSample> sample = timeSpentPerProjectLogger.drain();
    if (sample.isEmpty()) {
      LOG.debug("Activity sample is empty. Doing nothing");
      return;
    }

    Map<String, TimeSpentSampleDto> dto = toTimeSpentDto(sample);
    String json = toJson(dto);

    SentStatus status = activitySampleHttpClient.sendTimeSpentSample(apiKey, json);
    if (status == ERROR) {
      LOG.debug("Error sending time spent sample. Caching it for future retries");
      unpublishedTimeSpentSamples.add(json);
    }
  }

  private void publishChangesSample(String apiKey) {
    Map<String, Map<String, ChangesSample>> sample = changesActivityTracker.drain();
    if (sample.isEmpty()) {
      LOG.debug("Changes sample is empty. Doing nothing");
      return;
    }

    Map<String, Map<String, ChangesSampleDto>> dto = toChangesDto(sample);
    String json = toJson(dto);

    SentStatus status = activitySampleHttpClient.sendChangesSample(apiKey, json);
    if (status == ERROR) {
      LOG.debug("Error sending changes sample. Caching it for future retries");
      unpublishedChangesSamples.add(json);
    }
  }

  private SentStatus publishUnpublishedSamples(String apiKey) {
    if (unpublishedTimeSpentSamples.isEmpty() && unpublishedChangesSamples.isEmpty()) {
      return OK;
    }

    for (int i = 0; i < unpublishedTimeSpentSamples.size(); i++) {
      String sample = unpublishedTimeSpentSamples.peek();
      SentStatus status = activitySampleHttpClient.sendTimeSpentSample(apiKey, sample);
      if (status == ERROR) {
        return ERROR;
      }
      unpublishedTimeSpentSamples.remove();
    }

    for (int i = 0; i < unpublishedChangesSamples.size(); i++) {
      String sample = unpublishedChangesSamples.peek();
      SentStatus status = activitySampleHttpClient.sendChangesSample(apiKey, sample);
      if (status == ERROR) {
        return ERROR;
      }
      unpublishedChangesSamples.remove();
    }

    return OK;
  }

  private static Map<String, Map<String, ChangesSampleDto>> toChangesDto(
      Map<String, Map<String, ChangesSample>> activity) {
    Map<String, Map<String, ChangesSampleDto>> sampleByProjectDto = new HashMap<>();

    for (Entry<String, Map<String, ChangesSample>> sampleByProject : activity.entrySet()) {
      Map<String, ChangesSampleDto> sampleByFileDto = new HashMap<>();

      for (Entry<String, ChangesSample> sampleByFile : sampleByProject.getValue().entrySet()) {
        ChangesSample sample = sampleByFile.getValue();
        ChangesSampleDto dto =
            new ChangesSampleDto(
                sample.samplingStartedAt(),
                sample.additions().get(),
                sample.removals().get(),
                sample.metadata());

        sampleByFileDto.put(sampleByFile.getKey(), dto);
      }

      sampleByProjectDto.put(sampleByProject.getKey(), sampleByFileDto);
    }

    return sampleByProjectDto;
  }

  private static Map<String, TimeSpentSampleDto> toTimeSpentDto(
      Map<String, TimeSpentPerProjectSample> activity) {
    Map<String, TimeSpentSampleDto> sampleByProjectName = new HashMap<>();

    for (Entry<String, TimeSpentPerProjectSample> entry : activity.entrySet()) {
      TimeSpentPerProjectSample sample = entry.getValue();
      TimeSpentSampleDto dto =
          new TimeSpentSampleDto(sample.samplingStartedAt(), sample.timeSpent().getSeconds());

      sampleByProjectName.put(entry.getKey(), dto);
    }

    return sampleByProjectName;
  }

  private <T> String toJson(T report) {
    try {
      return OBJECT_MAPPER.writeValueAsString(report);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed to convert report to JSON", ex);
    }
  }

  @Override
  public void dispose() {
    LOG.info("Disposing DataReportingTask - flushing accumulated data before shutdown");

    try {
      // Perform final flush to prevent data loss
      flushActivityData();
      LOG.info("Final flush completed successfully");
    } catch (Exception e) {
      LOG.warn("Error during final flush before shutdown", e);
    }

    // Cancel the scheduled task
    if (task != null) {
      task.cancel(false);
    }

    LOG.info("DataReportingTask disposed successfully");
  }
}
