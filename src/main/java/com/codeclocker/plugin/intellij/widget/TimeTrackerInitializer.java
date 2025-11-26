package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_REMOVALS;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.Config;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient.DailyTimeResponse;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient.ProjectStats;
import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimeTrackerInitializer {

  private static final Logger LOG = Logger.getInstance(TimeTrackerInitializer.class);

  private static final AtomicReference<ScheduledFuture<?>> retryTask = new AtomicReference<>(null);
  private static volatile boolean refetchedAfterApiKeyIsSet = false;
  private static volatile boolean initialized;

  public static void initializeTimerWidgets() {
    ApplicationManager.getApplication()
        .executeOnPooledThread(TimeTrackerInitializer::fetchTimeAndInitialize);
  }

  public static void reinitializeTimerWidgetsRefetchingDataFromHub() {
    if (refetchedAfterApiKeyIsSet) {
      LOG.debug("Already refetched after API key was set, skipping");
      return;
    }

    LOG.info("Refetching data from Hub after API key was set");
    refetchedAfterApiKeyIsSet = true;
    ApplicationManager.getApplication()
        .executeOnPooledThread(TimeTrackerInitializer::fetchTimeAndInitialize);
  }

  public static void markApiKeyAsChanged() {
    refetchedAfterApiKeyIsSet = false;
  }

  private static void fetchTimeAndInitialize() {
    try {
      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      if (isBlank(apiKey)) {
        LOG.debug("No active API key");
        cancelRetryTask();
        initializeAllProjectWidgets(Map.of(), false);
        return;
      }

      DailyTimeHttpClient httpClient =
          ApplicationManager.getApplication().getService(DailyTimeHttpClient.class);
      DailyTimeResponse response = getDailyTimeFromHub(httpClient, apiKey);
      if (response.isError()) {
        LOG.warn("Failed to fetch daily time, initializing widgets with 0 and starting retry task");
        initializeAllProjectWidgets(Map.of(), false);
        startRetryTask();
        return;
      }

      if (response.isSubscriptionExpired()) {
        LOG.info("Subscription expired, showing exclamation mark in widgets");
        cancelRetryTask();
        initializeAllProjectWidgets(Map.of(), true);
        return;
      }

      Map<String, DailyTimeHttpClient.ProjectStats> projectStats = response.getProjects();
      LOG.info("Fetched daily time for project count: " + projectStats.size());
      cancelRetryTask();
      initializeAllProjectWidgets(projectStats, false);
    } catch (Exception e) {
      LOG.error("Error initializing timer widgets", e);
      initializeAllProjectWidgets(Map.of(), false);
      startRetryTask();
    }
  }

  private static DailyTimeHttpClient.DailyTimeResponse getDailyTimeFromHub(
      DailyTimeHttpClient httpClient, String apiKey) {
    DailyTimeResponse response =
        httpClient.fetchDailyTimePerProject(apiKey, ZoneId.systemDefault());

    if (response.isError()) {
      LOG.warn("Failed to fetch daily time, retrying");
      return httpClient.fetchDailyTimePerProject(apiKey, ZoneId.systemDefault());
    }

    return response;
  }

  private static void initializeAllProjectWidgets(
      Map<String, DailyTimeHttpClient.ProjectStats> projectStats, boolean subscriptionExpired) {
    if (isEmpty(projectStats) && initialized) {
      LOG.debug(
          "Skipping reinitializing timers with empty project stats since they are already initialized");
      return;
    }

    long totalTime =
        projectStats.values().stream()
            .mapToLong(DailyTimeHttpClient.ProjectStats::timeSpentSeconds)
            .sum();
    long totalAdditions =
        projectStats.values().stream().mapToLong(DailyTimeHttpClient.ProjectStats::additions).sum();
    long totalRemovals =
        projectStats.values().stream().mapToLong(DailyTimeHttpClient.ProjectStats::removals).sum();

    LOG.debug(
        "Total time across all projects: {}s, additions: {}, removals: {}",
        totalTime,
        totalAdditions,
        totalRemovals);

    // Reset the global stopwatch since the backend total already includes all accumulated time
    GLOBAL_STOP_WATCH.reset();

    // Initialize global VCS counters with data from backend
    GLOBAL_ADDITIONS.set(totalAdditions);
    GLOBAL_REMOVALS.set(totalRemovals);
    LOG.debug(
        "Initialized GLOBAL_ADDITIONS: {}, GLOBAL_REMOVALS: {}", totalAdditions, totalRemovals);

    initializeVcsChanges(projectStats);
    initializeCodingTime(projectStats, totalTime);

    initialized = true;
  }

  private static void initializeCodingTime(Map<String, ProjectStats> projectStats, long totalTime) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      String projectName = project.getName();
      ProjectStats stats = projectStats.get(projectName);
      long initialSeconds = stats != null ? stats.timeSpentSeconds() : 0L;

      TimeTrackerWidgetService service = project.getService(TimeTrackerWidgetService.class);
      if (service != null) {
        service.initialize(initialSeconds, totalTime);
        LOG.debug(
            "Initialized timer widget for project {} with {}s (total: {}s)",
            projectName,
            initialSeconds,
            totalTime);
      }
    }
  }

  private static void initializeVcsChanges(Map<String, ProjectStats> projectStats) {
    ChangesActivityTracker changesTracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    changesTracker.clearAllProjectChanges();

    for (Map.Entry<String, ProjectStats> entry : projectStats.entrySet()) {
      String projectName = entry.getKey();
      ProjectStats stats = entry.getValue();
      changesTracker.initializeProjectChanges(projectName, stats.additions(), stats.removals());
    }
  }

  private static synchronized void startRetryTask() {
    if (retryTask.get() != null && !retryTask.get().isDone()) {
      LOG.debug("Retry task already running, skipping");
      return;
    }

    int retryIntervalSeconds = Config.getApiFetchRetryIntervalSeconds();
    LOG.info("Starting retry task with interval: " + retryIntervalSeconds + "s");

    retryTask.set(
        AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(
                () -> {
                  try {
                    LOG.debug("Retry task executing - attempting to fetch data from API");
                    fetchTimeAndInitialize();
                  } catch (Exception e) {
                    LOG.error("Error in retry task", e);
                  }
                },
                retryIntervalSeconds,
                retryIntervalSeconds,
                TimeUnit.SECONDS));
  }

  private static synchronized void cancelRetryTask() {
    if (retryTask.get() != null && !retryTask.get().isDone()) {
      LOG.info("Cancelling retry task - data successfully fetched");
      retryTask.get().cancel(false);
      retryTask.set(null);
    }
  }
}
