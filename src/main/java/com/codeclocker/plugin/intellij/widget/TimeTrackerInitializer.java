package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.services.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.ChangesActivityTracker.GLOBAL_REMOVALS;
import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.reporting.DailyTimeHttpClient;
import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import java.time.ZoneId;
import java.util.Map;

public class TimeTrackerInitializer {

  private static final Logger LOG = Logger.getInstance(TimeTrackerInitializer.class);

  private static volatile boolean refetchedAfterApiKeyIsSet = false;

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
        initializeAllProjectWidgets(Map.of(), false);
        return;
      }

      DailyTimeHttpClient httpClient =
          ApplicationManager.getApplication().getService(DailyTimeHttpClient.class);
      DailyTimeHttpClient.DailyTimeResponse response =
          httpClient.fetchDailyTimePerProject(apiKey, ZoneId.systemDefault());

      if (response.isError()) {
        LOG.warn("Failed to fetch daily time, initializing widgets with 0");
        initializeAllProjectWidgets(Map.of(), false);
        return;
      }

      if (response.isSubscriptionExpired()) {
        LOG.info("Subscription expired, showing exclamation mark in widgets");
        initializeAllProjectWidgets(Map.of(), true);
        return;
      }

      Map<String, DailyTimeHttpClient.ProjectStats> projectStats = response.getProjects();
      LOG.info("Fetched daily time for project count: " + projectStats.size());
      initializeAllProjectWidgets(projectStats, false);
    } catch (Exception e) {
      LOG.error("Error initializing timer widgets", e);
      initializeAllProjectWidgets(Map.of(), false);
    }
  }

  private static void initializeAllProjectWidgets(
      Map<String, DailyTimeHttpClient.ProjectStats> projectStats, boolean subscriptionExpired) {
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

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      String projectName = project.getName();
      DailyTimeHttpClient.ProjectStats stats = projectStats.get(projectName);
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
}
