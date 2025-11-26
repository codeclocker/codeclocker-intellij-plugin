package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle.isActivityDataStoppedBeingCollected;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.Config;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.codeclocker.plugin.intellij.services.ChangesSample;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectSample;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.subscription.CheckSubscriptionStateHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public final class DataReportingTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(CheckSubscriptionStateHttpClient.class);

  private final int flushToServerFrequencySeconds;
  private final TimeSpentPerProjectLogger timeSpentPerProjectLogger;
  private final ChangesActivityTracker changesActivityTracker;
  private final ActivitySampleHttpClient activitySampleHttpClient;
  private final Queue<String> unpublishedTimeSpentSamples = new ArrayDeque<>();
  private final Queue<String> unpublishedChangesSamples = new ArrayDeque<>();

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

      // Validate timers before flushing to detect inconsistencies
      validateTimersBeforeFlush();

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
      sendActivitySampleToServer();
      LOG.info("Final flush completed successfully");
    } catch (Exception e) {
      LOG.warn("Error during final flush before shutdown", e);
    }

    // Shutdown the executor gracefully
    EXECUTOR.shutdown();

    try {
      // Wait up to 5 seconds for any remaining tasks to complete
      if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.warn("Executor did not terminate within 5 seconds, forcing shutdown");
        EXECUTOR.shutdownNow();

        // Wait a bit more for tasks to respond to being cancelled
        if (!EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
          LOG.error("Executor did not terminate even after shutdownNow");
        }
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for executor termination", e);
      EXECUTOR.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOG.info("DataReportingTask disposed successfully");
  }
}
