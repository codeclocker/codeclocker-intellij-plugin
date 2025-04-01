package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle.isActivityDataStoppedBeingCollected;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.apikey.CheckApiKeyStateHttpClient;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.services.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.services.ChangesSample;
import com.codeclocker.plugin.intellij.services.TimeSpentActivityTracker;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectSample;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

public final class DataReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(CheckApiKeyStateHttpClient.class);

  private final int flushToServerFrequencySeconds;
  private final TimeSpentActivityTracker timeSpentActivityTracker;
  private final ChangesActivityTracker changesActivityTracker;
  private final ActivitySampleHttpClient activitySampleHttpClient;
  private final Queue<String> unpublishedTimeSpentSamples = new ArrayDeque<>();
  private final Queue<String> unpublishedChangesSamples = new ArrayDeque<>();

  public DataReportingTask() {
    this.changesActivityTracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    this.timeSpentActivityTracker =
        ApplicationManager.getApplication().getService(TimeSpentActivityTracker.class);
    this.activitySampleHttpClient =
        ApplicationManager.getApplication().getService(ActivitySampleHttpClient.class);
    ConfigProvider configProvider =
        ApplicationManager.getApplication().getService(ConfigProvider.class);
    this.flushToServerFrequencySeconds = configProvider.getActivityDataFlushFrequencySeconds();
  }

  public void schedule() {
    EXECUTOR.scheduleWithFixedDelay(
        this::sendActivitySampleToServer,
        flushToServerFrequencySeconds,
        flushToServerFrequencySeconds,
        SECONDS);
  }

  private void sendActivitySampleToServer() {
    try {
      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      if (shouldSkipSendingActivityData(apiKey)) {
        return;
      }

      SentStatus unpublishedSamplesPublishStatus = publishUnpublishedSamples(apiKey);
      if (unpublishedSamplesPublishStatus == ERROR) {
        LOG.debug("Failed to publish unpublished samples");
        return;
      }

      publishTimeSpentSample(apiKey);
      publishChangesSample(apiKey);
    } catch (Exception ex) {
      LOG.debug("Error sending activity sample: {}", ex.getMessage());
    }
  }

  private void publishTimeSpentSample(String apiKey) {
    Map<String, TimeSpentPerProjectSample> sample = timeSpentActivityTracker.drain();
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

  private static boolean shouldSkipSendingActivityData(String apiKey) {
    return isBlank(apiKey) || isActivityDataStoppedBeingCollected();
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
          new TimeSpentSampleDto(
              sample.samplingStartedAt(),
              Duration.ofNanos(sample.timeSpent().get().getNanoTime()).toSeconds());

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
    EXECUTOR.shutdown();
  }
}
