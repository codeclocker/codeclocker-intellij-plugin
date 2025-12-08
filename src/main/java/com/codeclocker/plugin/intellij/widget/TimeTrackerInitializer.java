package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_INIT_SECONDS;
import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;
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
import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
        LOG.debug("No active API key, initializing from local state");
        cancelRetryTask();
        initializeFromLocalState();
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

    long totalGlobalSeconds =
        projectStats.values().stream()
            .mapToLong(DailyTimeHttpClient.ProjectStats::timeSpentSeconds)
            .sum();
    long totalAdditions =
        projectStats.values().stream().mapToLong(DailyTimeHttpClient.ProjectStats::additions).sum();
    long totalRemovals =
        projectStats.values().stream().mapToLong(DailyTimeHttpClient.ProjectStats::removals).sum();

    LOG.debug(
        "Total time across all projects: {}s, additions: {}, removals: {}",
        totalGlobalSeconds,
        totalAdditions,
        totalRemovals);

    // Reset the global stopwatch since the backend total already includes all accumulated time
    GLOBAL_STOP_WATCH.reset();
    GLOBAL_INIT_SECONDS.set(totalGlobalSeconds);

    // Initialize global VCS counters with data from backend
    GLOBAL_ADDITIONS.set(totalAdditions);
    GLOBAL_REMOVALS.set(totalRemovals);
    LOG.debug(
        "Initialized GLOBAL_ADDITIONS: {}, GLOBAL_REMOVALS: {}", totalAdditions, totalRemovals);

    initializeVcsChanges(projectStats);
    initializeCodingTime(projectStats);

    initialized = true;
  }

  private static void initializeCodingTime(Map<String, ProjectStats> projectStats) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      String projectName = project.getName();
      ProjectStats stats = projectStats.get(projectName);
      long initialSeconds = stats != null ? stats.timeSpentSeconds() : 0L;

      initializeTimeTrackerWidget(
          project, initialSeconds, "Initialized timer widget for project {} with {}s", projectName);
    }
  }

  private static void initializeTimeTrackerWidget(
      Project project, long initialProjectSeconds, String message, String projectName) {
    TimeTrackerWidgetService service = project.getService(TimeTrackerWidgetService.class);
    if (service != null) {
      service.initialize(initialProjectSeconds);
      LOG.debug(message, projectName, initialProjectSeconds);
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

  private static void initializeFromLocalState() {
    LocalStateRepository localState =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);
    Map<String, ProjectActivitySnapshot> todayStats = aggregateTodayStats(localState);

    if (todayStats.isEmpty() && initialized) {
      LOG.debug("No local state data for today and already initialized, skipping");
      return;
    }

    long totalTime =
        todayStats.values().stream().mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds).sum();
    long totalAdditions =
        todayStats.values().stream().mapToLong(ProjectActivitySnapshot::getAdditions).sum();
    long totalRemovals =
        todayStats.values().stream().mapToLong(ProjectActivitySnapshot::getRemovals).sum();

    LOG.info(
        "Initializing from local state - total time: "
            + totalTime
            + "s, additions: "
            + totalAdditions
            + ", removals: "
            + totalRemovals);

    GLOBAL_STOP_WATCH.reset();
    GLOBAL_INIT_SECONDS.set(totalTime);
    GLOBAL_ADDITIONS.set(totalAdditions);
    GLOBAL_REMOVALS.set(totalRemovals);

    initializeVcsChangesFromLocalState(todayStats);
    initializeCodingTimeFromLocalState(todayStats);

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

  private static void initializeCodingTimeFromLocalState(
      Map<String, ProjectActivitySnapshot> projectStats) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      String projectName = project.getName();
      ProjectActivitySnapshot stats = projectStats.get(projectName);
      long initialSeconds = stats != null ? stats.getCodedTimeSeconds() : 0L;

      initializeTimeTrackerWidget(
          project,
          initialSeconds,
          "Initialized timer widget from local state for project {} with {}s (total: {}s)",
          projectName);
    }
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
