package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_REMOVALS;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.config.Config;
import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient.DailyTimeResponse;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient.ProjectStats;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Initializes VCS counters (additions/removals) from local state or hub on startup. Time tracking
 * data is read directly from LocalStateRepository by the widget, so no initialization is needed.
 */
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
        LOG.debug("No active API key, initializing from local state");
        cancelRetryTask();
        initializeFromLocalState();
        return;
      }

      DailyTimeHttpClient httpClient =
          ApplicationManager.getApplication().getService(DailyTimeHttpClient.class);
      DailyTimeResponse response = getDailyTimeFromHub(httpClient, apiKey);
      if (response.isError()) {
        LOG.warn(
            "Failed to fetch daily time, initializing from local state and starting retry task");
        initializeFromLocalState();
        startRetryTask();
        return;
      }

      if (response.isSubscriptionExpired()) {
        LOG.info("Subscription expired, initializing from local state");
        cancelRetryTask();
        initializeFromLocalState();
        return;
      }

      Map<String, DailyTimeHttpClient.ProjectStats> projectStats = response.getProjects();
      LOG.info("Fetched daily time for project count: " + projectStats.size());
      cancelRetryTask();
      initializeFromHubData(projectStats);
    } catch (Exception e) {
      LOG.error("Error initializing timer widgets", e);
      initializeFromLocalState();
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

  private static void initializeFromHubData(Map<String, ProjectStats> projectStats) {
    if (initialized) {
      LOG.debug("Already initialized, skipping re-initialization from hub data");
      return;
    }

    if (isEmpty(projectStats)) {
      LOG.debug("No project stats from hub, skipping initialization");
      initialized = true;
      return;
    }

    // Calculate totals for VCS counters
    long totalAdditions = 0;
    long totalRemovals = 0;

    for (Map.Entry<String, ProjectStats> entry : projectStats.entrySet()) {
      ProjectStats stats = entry.getValue();
      totalAdditions += stats.additions();
      totalRemovals += stats.removals();
    }

    LOG.debug(
        "Initializing VCS counters from hub: additions: {}, removals: {}",
        totalAdditions,
        totalRemovals);

    // Initialize global VCS counters with data from backend
    // Note: Time data is now read directly from LocalStateRepository, no need to load into
    // accumulators
    GLOBAL_ADDITIONS.set(totalAdditions);
    GLOBAL_REMOVALS.set(totalRemovals);

    initializeVcsChanges(projectStats);

    initialized = true;
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

  private static void initializeFromLocalState() {
    if (initialized) {
      LOG.debug("Already initialized, skipping re-initialization from local state");
      return;
    }

    LocalStateRepository localState =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);
    Map<String, ProjectActivitySnapshot> todayStats = aggregateTodayStats(localState);

    // Calculate VCS totals from local state
    long totalAdditions = 0;
    long totalRemovals = 0;

    for (Map.Entry<String, ProjectActivitySnapshot> entry : todayStats.entrySet()) {
      ProjectActivitySnapshot stats = entry.getValue();
      totalAdditions += stats.getAdditions();
      totalRemovals += stats.getRemovals();
    }

    long totalTime = localState.getTodayTotalSeconds();
    LOG.info(
        "Initializing VCS counters from local state - total time: "
            + totalTime
            + "s, additions: "
            + totalAdditions
            + ", removals: "
            + totalRemovals);

    // Note: Time data is now read directly from LocalStateRepository, no need to load into
    // accumulators
    GLOBAL_ADDITIONS.set(totalAdditions);
    GLOBAL_REMOVALS.set(totalRemovals);

    initializeVcsChangesFromLocalState(todayStats);

    initialized = true;
  }

  private static Map<String, ProjectActivitySnapshot> aggregateTodayStats(
      LocalStateRepository localState) {
    String todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    Map<String, ProjectActivitySnapshot> aggregated = new HashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry :
        localState.getAllData().entrySet()) {
      if (!hourEntry.getKey().startsWith(todayPrefix)) {
        continue;
      }

      for (Map.Entry<String, ProjectActivitySnapshot> projectEntry :
          hourEntry.getValue().entrySet()) {
        String projectName = projectEntry.getKey();
        ProjectActivitySnapshot snapshot = projectEntry.getValue();

        aggregated.merge(
            projectName,
            snapshot,
            (existing, incoming) ->
                new ProjectActivitySnapshot(
                    existing.getCodedTimeSeconds() + incoming.getCodedTimeSeconds(),
                    existing.getAdditions() + incoming.getAdditions(),
                    existing.getRemovals() + incoming.getRemovals(),
                    existing.isReported()));
      }
    }

    return aggregated;
  }

  private static void initializeVcsChangesFromLocalState(
      Map<String, ProjectActivitySnapshot> projectStats) {
    ChangesActivityTracker changesTracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    changesTracker.clearAllProjectChanges();

    for (Map.Entry<String, ProjectActivitySnapshot> entry : projectStats.entrySet()) {
      String projectName = entry.getKey();
      ProjectActivitySnapshot stats = entry.getValue();
      changesTracker.initializeProjectChanges(
          projectName, stats.getAdditions(), stats.getRemovals());
    }
  }
}
