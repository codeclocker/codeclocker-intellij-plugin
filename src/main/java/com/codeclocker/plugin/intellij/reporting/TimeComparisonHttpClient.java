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

public class TimeComparisonHttpClient {

  private static final Logger LOG = Logger.getInstance(TimeComparisonHttpClient.class);

  private final ApiKeyLifecycle apiKeyLifecycle;

  public TimeComparisonHttpClient() {
    this.apiKeyLifecycle = ApplicationManager.getApplication().getService(ApiKeyLifecycle.class);
  }

  public TimeComparisonResponse fetchTimeComparison(
      String apiKey,
      Instant currentFrom,
      Instant currentTo,
      Instant previousFrom,
      Instant previousTo) {
    try {
      String url = buildUrl(currentFrom, currentTo, previousFrom, previousTo);
      LOG.debug("Fetching time comparison from: {}", url);

      String response =
          HttpRequests.request(url)
              .connectTimeout(CONNECT_TIMEOUT)
              .readTimeout(READ_TIMEOUT)
              .tuner(connection -> connection.setRequestProperty("X-codeclocker-api-key", apiKey))
              .readString();

      LOG.debug("Received response: {}", response);

      if (response.contains("Activity data stopped being collected")) {
        apiKeyLifecycle.processHubErrorResponse(response);
        return TimeComparisonResponse.subscriptionExpired();
      }

      TimePeriodComparisonDto dto =
          OBJECT_MAPPER.readValue(response, TimePeriodComparisonDto.class);
      return TimeComparisonResponse.success(dto);
    } catch (IOException ex) {
      LOG.warn("Error fetching time comparison: " + ex.getMessage(), ex);
      return TimeComparisonResponse.error();
    }
  }

  private static String buildUrl(
      Instant currentFrom, Instant currentTo, Instant previousFrom, Instant previousTo) {
    return HUB_API_HOST
        + "/api/v1/plugin/time-comparison?"
        + "currentFrom="
        + encode(currentFrom.toString())
        + "&currentTo="
        + encode(currentTo.toString())
        + "&previousFrom="
        + encode(previousFrom.toString())
        + "&previousTo="
        + encode(previousTo.toString());
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public record TimePeriodComparisonDto(
      long currentPeriodSeconds,
      long previousPeriodSeconds,
      long differenceSeconds,
      int percentageChange) {}

  public static class TimeComparisonResponse {
    private final TimePeriodComparisonDto comparison;
    private final boolean subscriptionExpired;
    private final boolean error;

    private TimeComparisonResponse(
        TimePeriodComparisonDto comparison, boolean subscriptionExpired, boolean error) {
      this.comparison = comparison;
      this.subscriptionExpired = subscriptionExpired;
      this.error = error;
    }

    public static TimeComparisonResponse success(TimePeriodComparisonDto comparison) {
      return new TimeComparisonResponse(comparison, false, false);
    }

    public static TimeComparisonResponse subscriptionExpired() {
      return new TimeComparisonResponse(null, true, false);
    }

    public static TimeComparisonResponse error() {
      return new TimeComparisonResponse(null, false, true);
    }

    public TimePeriodComparisonDto getComparison() {
      return comparison;
    }

    public boolean isSubscriptionExpired() {
      return subscriptionExpired;
    }

    public boolean isError() {
      return error;
    }

    public boolean isSuccess() {
      return !subscriptionExpired && !error && comparison != null;
    }
  }
}
