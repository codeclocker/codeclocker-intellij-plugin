package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

public class DailyTimeHttpClient {

  private static final Logger LOG = Logger.getInstance(DailyTimeHttpClient.class);

  private final ApiKeyLifecycle apiKeyLifecycle;

  public DailyTimeHttpClient() {
    this.apiKeyLifecycle = ApplicationManager.getApplication().getService(ApiKeyLifecycle.class);
  }

  public DailyTimeResponse fetchDailyTimePerProject(String apiKey, ZoneId timeZone) {
    try {
      Instant startOfToday = getStartOfToday(timeZone);

      String fromParam = URLEncoder.encode(startOfToday.toString(), StandardCharsets.UTF_8);
      String url = HUB_API_HOST + "/api/v1/plugin/daily-time-per-project?from=" + fromParam;
      LOG.debug("Fetching daily time from: {} (start of today: {})", url, startOfToday);

      String response =
          HttpRequests.request(url)
              .connectTimeout(CONNECT_TIMEOUT)
              .readTimeout(READ_TIMEOUT)
              .tuner(connection -> connection.setRequestProperty("X-codeclocker-api-key", apiKey))
              .readString();

      LOG.debug("Received response: {}", response);

      // Check for error messages
      if (response.contains("Activity data stopped being collected")) {
        apiKeyLifecycle.processHubErrorResponse(response);
        return DailyTimeResponse.subscriptionExpired();
      }

      // Normal response - parse the projects map
      DailyTimePerProjectDto dto = OBJECT_MAPPER.readValue(response, DailyTimePerProjectDto.class);
      return DailyTimeResponse.success(dto.projects());
    } catch (IOException ex) {
      LOG.warn("Error fetching daily time: " + ex.getMessage(), ex);
      return DailyTimeResponse.error();
    }
  }

  private static Instant getStartOfToday(ZoneId timeZone) {
    return ZonedDateTime.of(LocalDate.now(timeZone).atStartOfDay(), timeZone).toInstant();
  }

  private record DailyTimePerProjectDto(Map<String, ProjectStats> projects) {}

  public record ProjectStats(long timeSpentSeconds, long additions, long removals) {}

  public static class DailyTimeResponse {
    private final Map<String, ProjectStats> projects;
    private final boolean subscriptionExpired;
    private final boolean error;

    private DailyTimeResponse(
        Map<String, ProjectStats> projects, boolean subscriptionExpired, boolean error) {
      this.projects = projects;
      this.subscriptionExpired = subscriptionExpired;
      this.error = error;
    }

    public static DailyTimeResponse success(Map<String, ProjectStats> projects) {
      return new DailyTimeResponse(projects, false, false);
    }

    public static DailyTimeResponse subscriptionExpired() {
      return new DailyTimeResponse(Collections.emptyMap(), true, false);
    }

    public static DailyTimeResponse error() {
      return new DailyTimeResponse(Collections.emptyMap(), false, true);
    }

    public Map<String, ProjectStats> getProjects() {
      return projects;
    }

    public boolean isSubscriptionExpired() {
      return subscriptionExpired;
    }

    public boolean isError() {
      return error;
    }
  }
}
