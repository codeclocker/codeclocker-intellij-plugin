package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.local.BranchActivityRecord;
import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.BranchActivityTracker;
import com.codeclocker.plugin.intellij.services.ChangesSample;
import com.codeclocker.plugin.intellij.services.CommitActivityTracker;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.ProjectTimeDelta;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;

@Service
public final class DataReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(DataReportingTask.class);

  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private final int flushToServerFrequencySeconds;
  private final TimeSpentPerProjectLogger timeSpentPerProjectLogger;
  private final ChangesActivityTracker changesActivityTracker;
  private final BranchActivityTracker branchActivityTracker;
  private final CommitActivityTracker commitActivityTracker;
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
    this.branchActivityTracker =
        ApplicationManager.getApplication().getService(BranchActivityTracker.class);
    this.commitActivityTracker =
        ApplicationManager.getApplication().getService(CommitActivityTracker.class);
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
      // Cleanup old local data periodically
      localStateRepository.rotate();

      // Get deltas - data stays in accumulators, just marks as reported
      Map<String, ProjectTimeDelta> timeDeltas = timeSpentPerProjectLogger.getProjectDeltas();
      Map<String, Map<String, ChangesSample>> changesSamples = changesActivityTracker.drain();

      if (timeDeltas.isEmpty() && changesSamples.isEmpty()) {
        LOG.debug("No activity data to save locally");
        return;
      }

      saveToLocalStorage(timeDeltas, changesSamples);

      // If API key is available, try to sync to server
      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      if (!isBlank(apiKey)) {
        sendActivitySampleToServer(apiKey, timeDeltas, changesSamples);
      }
    } catch (Exception ex) {
      LOG.debug("Error flushing activity data: {}", ex.getMessage());
    }
  }

  public void saveToLocalStorageIfApiKeyIsEmpty() {
    String apiKey = ApiKeyLifecycle.getActiveApiKey();
    if (isBlank(apiKey)) {
      Map<String, ProjectTimeDelta> timeDeltas = timeSpentPerProjectLogger.getProjectDeltas();
      Map<String, Map<String, ChangesSample>> changesSamples = changesActivityTracker.drain();
      if (timeDeltas.isEmpty() && changesSamples.isEmpty()) {
        LOG.debug("No activity data to save locally");
        return;
      }

      saveToLocalStorage(timeDeltas, changesSamples);
    }
  }

  public void saveToLocalStorage(
      Map<String, ProjectTimeDelta> timeDeltas,
      Map<String, Map<String, ChangesSample>> changesSamples) {

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

    String currentHourKey =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

    // Save time spent per project (using delta seconds)
    for (Entry<String, ProjectTimeDelta> entry : timeDeltas.entrySet()) {
      String projectName = entry.getKey();
      long deltaSeconds = entry.getValue().deltaSeconds();
      long additions = projectAdditions.getOrDefault(projectName, 0L);
      long removals = projectRemovals.getOrDefault(projectName, 0L);

      ProjectActivitySnapshot snapshot =
          new ProjectActivitySnapshot(deltaSeconds, additions, removals, false);

      // Add branch activity
      if (branchActivityTracker != null) {
        Map<String, Long> branchActivity =
            branchActivityTracker.drainBranchActivity(projectName, currentHourKey);
        List<BranchActivityRecord> branchRecords = new ArrayList<>();
        for (Entry<String, Long> branchEntry : branchActivity.entrySet()) {
          branchRecords.add(new BranchActivityRecord(branchEntry.getKey(), branchEntry.getValue()));
        }
        snapshot.setBranchActivity(branchRecords);
      }

      // Add commits
      if (commitActivityTracker != null) {
        List<CommitRecord> commits =
            commitActivityTracker.drainCommits(projectName, currentHourKey);
        snapshot.setCommits(commits);
      }

      localStateRepository.mergeProjectCurrentHour(projectName, snapshot);
    }

    // Save VCS changes for projects without time entries
    for (String projectName : projectAdditions.keySet()) {
      if (!timeDeltas.containsKey(projectName)) {
        long additions = projectAdditions.get(projectName);
        long removals = projectRemovals.get(projectName);

        ProjectActivitySnapshot snapshot =
            new ProjectActivitySnapshot(0, additions, removals, false);
        localStateRepository.mergeProjectCurrentHour(projectName, snapshot);
      }
    }

    LOG.debug("Saved activity data to local storage for " + timeDeltas.size() + " projects");
  }

  private void sendActivitySampleToServer(
      String apiKey,
      Map<String, ProjectTimeDelta> timeDeltas,
      Map<String, Map<String, ChangesSample>> changesSamples) {

    // First, sync any locally stored data to the server
    syncLocalDataToServer(apiKey);

    SentStatus unpublishedSamplesPublishStatus = publishUnpublishedSamples(apiKey);
    if (unpublishedSamplesPublishStatus == ERROR) {
      LOG.debug("Failed to publish unpublished samples");
      return;
    }

    publishTimeSpentSample(apiKey, timeDeltas);
    publishChangesSample(apiKey, changesSamples);
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

    // Send data hour by hour to preserve time distribution
    // Map: hourKey -> (projectName -> TimeSpentSampleDto)
    Map<String, Map<String, TimeSpentSampleDto>> timeSpentByHour = new HashMap<>();
    // For changes, we send per-project with hour info in filename
    Map<String, Map<String, ChangesSampleDto>> changesByProject = new HashMap<>();

    for (Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : localData.entrySet()) {
      String hourKey = hourEntry.getKey();
      long samplingStartedAt = datetimeHourToTimestamp(hourKey);

      for (Entry<String, ProjectActivitySnapshot> projectEntry : hourEntry.getValue().entrySet()) {
        String projectName = projectEntry.getKey();
        ProjectActivitySnapshot snapshot = projectEntry.getValue();

        // Keep time spent data per hour per project (hourKey is already in UTC)
        if (snapshot.getCodedTimeSeconds() > 0) {
          timeSpentByHour
              .computeIfAbsent(hourKey, k -> new HashMap<>())
              .put(
                  projectName,
                  new TimeSpentSampleDto(
                      snapshot.getRecordId(),
                      hourKey,
                      snapshot.getCodedTimeSeconds(),
                      snapshot.getCodedTimeSeconds()));
        }

        // Aggregate VCS changes per project (with hour in filename for tracking)
        if (snapshot.getAdditions() > 0 || snapshot.getRemovals() > 0) {
          String syntheticFileName = "local-sync-" + hourKey;
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

    // Send time spent data hour by hour
    boolean timeSyncSuccess = true;
    int totalProjectsSynced = 0;
    for (Entry<String, Map<String, TimeSpentSampleDto>> hourEntry : timeSpentByHour.entrySet()) {
      Map<String, TimeSpentSampleDto> projectsForHour = hourEntry.getValue();
      String timeJson = toJson(projectsForHour);
      SentStatus status = activitySampleHttpClient.sendTimeSpentSample(apiKey, timeJson);
      if (status == ERROR) {
        LOG.warn("Failed to sync local time spent data for hour " + hourEntry.getKey());
        timeSyncSuccess = false;
        break;
      }
      totalProjectsSynced += projectsForHour.size();
    }
    if (timeSyncSuccess && totalProjectsSynced > 0) {
      LOG.info(
          "Synced local time spent data: "
              + timeSpentByHour.size()
              + " hours, "
              + totalProjectsSynced
              + " project entries");
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

  private void publishTimeSpentSample(String apiKey, Map<String, ProjectTimeDelta> deltas) {
    if (deltas.isEmpty()) {
      return;
    }

    Map<String, TimeSpentSampleDto> dto = toTimeSpentDto(deltas);
    String json = toJson(dto);

    SentStatus status = activitySampleHttpClient.sendTimeSpentSample(apiKey, json);
    if (status == ERROR) {
      LOG.debug("Error sending time spent sample. Caching it for future retries");
      unpublishedTimeSpentSamples.add(json);
    }
  }

  private void publishChangesSample(String apiKey, Map<String, Map<String, ChangesSample>> sample) {
    if (sample.isEmpty()) {
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
      Map<String, ProjectTimeDelta> deltas) {
    Map<String, TimeSpentSampleDto> sampleByProjectName = new HashMap<>();

    for (Entry<String, ProjectTimeDelta> entry : deltas.entrySet()) {
      ProjectTimeDelta delta = entry.getValue();
      // hourKey is already in UTC (from ProjectTimeAccumulator)
      // recordId is null here - live deltas use traditional ADD behavior on Hub
      // Local storage sync uses recordId for idempotent REPLACE behavior
      TimeSpentSampleDto dto =
          new TimeSpentSampleDto(
              null, delta.hourKey(), delta.deltaSeconds(), delta.totalHourSeconds());

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
