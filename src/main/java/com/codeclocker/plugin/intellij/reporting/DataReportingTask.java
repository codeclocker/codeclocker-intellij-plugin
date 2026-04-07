package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.local.BranchActivityRecord;
import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.local.FileChangeRecord;
import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.reporting.TimeSpentSampleDto.BranchActivityDto;
import com.codeclocker.plugin.intellij.reporting.TimeSpentSampleDto.CommitDto;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

@Service
public final class DataReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(DataReportingTask.class);

  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private ScheduledFuture<?> task;

  private ChangesActivityTracker getChangesActivityTracker() {
    return ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
  }

  private TimeSpentPerProjectLogger getTimeSpentPerProjectLogger() {
    return ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
  }

  private BranchActivityTracker getBranchActivityTracker() {
    return ApplicationManager.getApplication().getService(BranchActivityTracker.class);
  }

  private CommitActivityTracker getCommitActivityTracker() {
    return ApplicationManager.getApplication().getService(CommitActivityTracker.class);
  }

  private ActivitySampleHttpClient getActivitySampleHttpClient() {
    return ApplicationManager.getApplication().getService(ActivitySampleHttpClient.class);
  }

  private LocalStateRepository getLocalStateRepository() {
    return ApplicationManager.getApplication().getService(LocalStateRepository.class);
  }

  private int getFlushToServerFrequencySeconds() {
    return ApplicationManager.getApplication()
        .getService(ConfigProvider.class)
        .getActivityDataFlushFrequencySeconds();
  }

  public void schedule() {
    if (task != null && !task.isCancelled()) {
      return;
    }

    int frequencySeconds = getFlushToServerFrequencySeconds();
    task =
        EXECUTOR.scheduleWithFixedDelay(
            this::flushActivityData, frequencySeconds, frequencySeconds, SECONDS);
  }

  public void flushActivityData() {
    try {
      getLocalStateRepository().rotate();

      Map<String, ProjectTimeDelta> timeDeltas = getTimeSpentPerProjectLogger().getProjectDeltas();
      Map<String, Map<String, ChangesSample>> changesSamples = getChangesActivityTracker().drain();

      if (timeDeltas.isEmpty() && changesSamples.isEmpty()) {
        LOG.debug("No activity data to save locally");
        return;
      }

      saveToLocalStorage(timeDeltas, changesSamples);

      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      if (!isBlank(apiKey)) {
        syncLocalDataToServer(apiKey);
      }
    } catch (Exception ex) {
      LOG.debug("Error flushing activity data: {}", ex.getMessage());
    }
  }

  public void saveToLocalStorageIfApiKeyIsEmpty() {
    String apiKey = ApiKeyLifecycle.getActiveApiKey();
    if (isBlank(apiKey)) {
      Map<String, ProjectTimeDelta> timeDeltas = getTimeSpentPerProjectLogger().getProjectDeltas();
      Map<String, Map<String, ChangesSample>> changesSamples = getChangesActivityTracker().drain();
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

    Map<String, Long> projectAdditions = new HashMap<>();
    Map<String, Long> projectRemovals = new HashMap<>();
    Map<String, List<FileChangeRecord>> projectFileChanges = new HashMap<>();

    for (Entry<String, Map<String, ChangesSample>> projectEntry : changesSamples.entrySet()) {
      String projectName = projectEntry.getKey();
      long totalAdditions = 0;
      long totalRemovals = 0;
      List<FileChangeRecord> fileRecords = new ArrayList<>();

      for (Entry<String, ChangesSample> fileEntry : projectEntry.getValue().entrySet()) {
        ChangesSample sample = fileEntry.getValue();
        long add = sample.additions().get();
        long rem = sample.removals().get();
        totalAdditions += add;
        totalRemovals += rem;

        String ext = sample.metadata().getOrDefault("extension", "");
        fileRecords.add(new FileChangeRecord(fileEntry.getKey(), add, rem, ext));
      }

      projectAdditions.put(projectName, totalAdditions);
      projectRemovals.put(projectName, totalRemovals);
      projectFileChanges.put(projectName, fileRecords);
    }

    String currentHourKey =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

    for (Entry<String, ProjectTimeDelta> entry : timeDeltas.entrySet()) {
      String projectName = entry.getKey();
      long deltaSeconds = entry.getValue().deltaSeconds();
      long additions = projectAdditions.getOrDefault(projectName, 0L);
      long removals = projectRemovals.getOrDefault(projectName, 0L);

      ProjectActivitySnapshot snapshot =
          new ProjectActivitySnapshot(deltaSeconds, additions, removals, false);
      snapshot.setFileChanges(projectFileChanges.getOrDefault(projectName, List.of()));

      BranchActivityTracker branchTracker = getBranchActivityTracker();
      if (branchTracker != null) {
        Map<String, Long> branchActivity =
            branchTracker.drainBranchActivity(projectName, currentHourKey);
        List<BranchActivityRecord> branchRecords = new ArrayList<>();
        for (Entry<String, Long> branchEntry : branchActivity.entrySet()) {
          branchRecords.add(new BranchActivityRecord(branchEntry.getKey(), branchEntry.getValue()));
        }
        snapshot.setBranchActivity(branchRecords);
      }

      CommitActivityTracker commitTracker = getCommitActivityTracker();
      if (commitTracker != null) {
        List<CommitRecord> commits = commitTracker.drainCommits(projectName, currentHourKey);
        snapshot.setCommits(commits);
      }

      getLocalStateRepository().mergeProjectCurrentHour(projectName, snapshot);
    }

    for (String projectName : projectAdditions.keySet()) {
      if (!timeDeltas.containsKey(projectName)) {
        long additions = projectAdditions.get(projectName);
        long removals = projectRemovals.get(projectName);

        ProjectActivitySnapshot snapshot =
            new ProjectActivitySnapshot(0, additions, removals, false);
        snapshot.setFileChanges(projectFileChanges.getOrDefault(projectName, List.of()));
        getLocalStateRepository().mergeProjectCurrentHour(projectName, snapshot);
      }
    }

    LOG.debug("Saved activity data to local storage for " + timeDeltas.size() + " projects");
  }

  public void syncLocalDataToServer(String apiKey) {
    LocalStateRepository localRepo = getLocalStateRepository();
    if (!localRepo.hasUnreportedData()) {
      return;
    }

    LOG.info("Found locally stored data, syncing to server...");

    Map<String, Map<String, ProjectActivitySnapshot>> localData = localRepo.getAllUnreportedData();
    if (localData.isEmpty()) {
      return;
    }

    Map<String, Map<String, TimeSpentSampleDto>> timeSpentByHour = new HashMap<>();
    Map<String, Map<String, ChangesSampleDto>> changesByProject = new HashMap<>();

    for (Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : localData.entrySet()) {
      String hourKey = hourEntry.getKey();
      long samplingStartedAt = datetimeHourToTimestamp(hourKey);

      for (Entry<String, ProjectActivitySnapshot> projectEntry : hourEntry.getValue().entrySet()) {
        String projectName = projectEntry.getKey();
        ProjectActivitySnapshot snapshot = projectEntry.getValue();

        List<BranchActivityDto> branchActivityDtos = null;
        if (snapshot.getBranchActivity() != null && !snapshot.getBranchActivity().isEmpty()) {
          branchActivityDtos =
              snapshot.getBranchActivity().stream()
                  .map(ba -> new BranchActivityDto(ba.getBranchName(), ba.getActiveSeconds()))
                  .toList();
        }

        List<CommitDto> commitDtos = null;
        if (snapshot.getCommits() != null && !snapshot.getCommits().isEmpty()) {
          commitDtos =
              snapshot.getCommits().stream()
                  .map(
                      c ->
                          new CommitDto(
                              c.getHash(),
                              c.getMessage(),
                              c.getAuthor(),
                              c.getTimestamp(),
                              c.getChangedFilesCount(),
                              c.getBranch()))
                  .toList();
        }

        if (snapshot.getCodedTimeSeconds() > 0
            || snapshot.getAdditions() > 0
            || snapshot.getRemovals() > 0
            || (branchActivityDtos != null && !branchActivityDtos.isEmpty())
            || (commitDtos != null && !commitDtos.isEmpty())) {
          timeSpentByHour
              .computeIfAbsent(hourKey, k -> new HashMap<>())
              .put(
                  projectName,
                  new TimeSpentSampleDto(
                      snapshot.getRecordId(),
                      hourKey,
                      snapshot.getCodedTimeSeconds(),
                      snapshot.getCodedTimeSeconds(),
                      snapshot.getAdditions(),
                      snapshot.getRemovals(),
                      branchActivityDtos,
                      commitDtos));
        }

        if (snapshot.getFileChanges() != null && !snapshot.getFileChanges().isEmpty()) {
          for (FileChangeRecord fc : snapshot.getFileChanges()) {
            if (fc.getAdditions() > 0 || fc.getRemovals() > 0) {
              Map<String, String> meta =
                  (fc.getExtension() != null && !fc.getExtension().isEmpty())
                      ? Map.of("extension", fc.getExtension())
                      : Collections.emptyMap();
              ChangesSampleDto changesDto =
                  new ChangesSampleDto(
                      samplingStartedAt, fc.getAdditions(), fc.getRemovals(), meta);
              changesByProject
                  .computeIfAbsent(projectName, k -> new HashMap<>())
                  .put(fc.getFileName(), changesDto);
            }
          }
        } else if (snapshot.getAdditions() > 0 || snapshot.getRemovals() > 0) {
          // Backward compat: old data without file-level detail
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

    ActivitySampleHttpClient httpClient = getActivitySampleHttpClient();

    boolean timeSyncSuccess = true;
    int totalProjectsSynced = 0;
    for (Entry<String, Map<String, TimeSpentSampleDto>> hourEntry : timeSpentByHour.entrySet()) {
      Map<String, TimeSpentSampleDto> projectsForHour = hourEntry.getValue();
      String timeJson = toJson(projectsForHour);
      SentStatus status = httpClient.sendTimeSpentSample(apiKey, timeJson);
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

    boolean changesSyncSuccess = true;
    if (!changesByProject.isEmpty()) {
      String changesJson = toJson(changesByProject);
      SentStatus status = httpClient.sendChangesSample(apiKey, changesJson);
      if (status == ERROR) {
        LOG.warn("Failed to sync local VCS changes data to server, will retry later");
        changesSyncSuccess = false;
      } else {
        LOG.info("Synced local VCS changes data for " + changesByProject.size() + " projects");
      }
    }

    if (timeSyncSuccess && changesSyncSuccess) {
      localRepo.markAllDataAsReported();
      LOG.info("Cleared local data after successful sync");
    }
  }

  private static long datetimeHourToTimestamp(String datetimeHourStr) {
    try {
      LocalDateTime dateTime = LocalDateTime.parse(datetimeHourStr, DATETIME_HOUR_FORMATTER);
      return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    } catch (Exception e) {
      return System.currentTimeMillis();
    }
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
      flushActivityData();
      LOG.info("Final flush completed successfully");
    } catch (Exception e) {
      LOG.warn("Error during final flush before shutdown", e);
    }

    if (task != null) {
      task.cancel(false);
    }

    LOG.info("DataReportingTask disposed successfully");
  }
}
