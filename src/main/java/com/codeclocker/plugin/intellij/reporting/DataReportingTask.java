package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle.isActivityDataStoppedBeingCollected;
import static com.codeclocker.plugin.intellij.reporting.ActivitySampleSendStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.ActivitySampleSendStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.apikey.CheckApiKeyStateHttpClient;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.services.ActivityTracker;
import com.codeclocker.plugin.intellij.services.TimeSpentPerFileLogger;
import com.codeclocker.plugin.intellij.services.TimeSpentPerFileSample;
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
  private final ActivityTracker activityTracker;
  private final ActivitySampleHttpClient activitySampleHttpClient;
  private final Queue<String> unpublishedSamples = new ArrayDeque<>();

  public DataReportingTask() {
    this.activityTracker = ApplicationManager.getApplication().getService(ActivityTracker.class);
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

      ActivitySampleSendStatus unpublishedSamplesPublishStatus = publishUnpublishedSamples(apiKey);
      if (unpublishedSamplesPublishStatus == ERROR) {
        return;
      }

      Map<String, Map<String, TimeSpentPerFileLogger>> sample =
          activityTracker.drainActivitySample();
      if (sample.isEmpty()) {
        LOG.debug("Activity sample is empty. Doing nothing");
        return;
      }

      Map<String, Map<String, Map<String, TimeSpentSampleDto>>> dto = toDto(sample);
      String json = toJson(dto);

      ActivitySampleSendStatus status = activitySampleHttpClient.send(apiKey, json);
      if (status == ERROR) {
        LOG.debug("Error sending activity sample. Caching it for future retries");
        unpublishedSamples.add(json);
      }
    } catch (Exception ex) {
      LOG.debug("Error sending activity sample: {}", ex.getMessage());
    }
  }

  private ActivitySampleSendStatus publishUnpublishedSamples(String apiKey) throws Exception {
    if (unpublishedSamples.isEmpty()) {
      return OK;
    }

    for (int i = 0; i < unpublishedSamples.size(); i++) {
      String sample = unpublishedSamples.peek();
      ActivitySampleSendStatus status = activitySampleHttpClient.send(apiKey, sample);
      if (status == ERROR) {
        return ERROR;
      }
      unpublishedSamples.remove();
    }

    return OK;
  }

  private static boolean shouldSkipSendingActivityData(String apiKey) {
    return isBlank(apiKey) || isActivityDataStoppedBeingCollected();
  }

  private static Map<String, Map<String, Map<String, TimeSpentSampleDto>>> toDto(
      Map<String, Map<String, TimeSpentPerFileLogger>> activity) {
    Map<String, Map<String, Map<String, TimeSpentSampleDto>>> moduleByProjectDto = new HashMap<>();

    for (Map.Entry<String, Map<String, TimeSpentPerFileLogger>> moduleByProject :
        activity.entrySet()) {
      Map<String, Map<String, TimeSpentSampleDto>> fileByModuleDto = new HashMap<>();

      for (Map.Entry<String, TimeSpentPerFileLogger> fileByModule :
          moduleByProject.getValue().entrySet()) {
        Map<String, TimeSpentSampleDto> convertedThirdMap = new HashMap<>();

        for (Entry<String, TimeSpentPerFileSample> sampleByFile :
            fileByModule.getValue().getTimingByElement().entrySet()) {
          convertedThirdMap.put(
              sampleByFile.getKey(), toTimeSpentSampleDto(sampleByFile.getValue()));
        }

        fileByModuleDto.put(fileByModule.getKey(), convertedThirdMap);
      }

      moduleByProjectDto.put(moduleByProject.getKey(), fileByModuleDto);
    }

    return moduleByProjectDto;
  }

  private static TimeSpentSampleDto toTimeSpentSampleDto(TimeSpentPerFileSample sample) {
    return new TimeSpentSampleDto(
        sample.samplingStartedAt(),
        Duration.ofNanos(sample.timeSpent().get().getNanoTime()).toSeconds(),
        sample.additions().get(),
        sample.removals().get(),
        sample.metadata());
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
