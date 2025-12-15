package com.codeclocker.plugin.intellij.analytics;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static com.intellij.util.io.HttpRequests.JSON_CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.reporting.SentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import java.io.IOException;

/** HTTP client for sending analytics reports to the backend. */
public class AnalyticsHttpClient {

  private static final Logger LOG = Logger.getInstance(AnalyticsHttpClient.class);

  private static final String ANALYTICS_ENDPOINT = "/api/v1/analytics/events";

  /**
   * Sends an analytics report to the backend.
   *
   * @param report The analytics report to send
   * @return SentStatus indicating success or failure
   */
  public SentStatus sendAnalyticsReport(AnalyticsReportDto report) {
    try {
      String jsonData = OBJECT_MAPPER.writeValueAsString(report);
      LOG.debug("Sending analytics report with " + report.events().size() + " events");

      String apiKey = ApiKeyLifecycle.getActiveApiKey();

      HttpRequests.post(HUB_API_HOST + ANALYTICS_ENDPOINT, JSON_CONTENT_TYPE)
          .connectTimeout(CONNECT_TIMEOUT)
          .readTimeout(READ_TIMEOUT)
          .tuner(
              connection -> {
                if (apiKey != null && !apiKey.isBlank()) {
                  connection.setRequestProperty("X-codeclocker-api-key", apiKey);
                }
              })
          .connect(
              request -> {
                request.write(jsonData.getBytes(UTF_8));
                return request.readString();
              });

      LOG.debug("Analytics report sent successfully");
      return SentStatus.OK;
    } catch (JsonProcessingException ex) {
      LOG.warn("Failed to serialize analytics report: " + ex.getMessage());
      return SentStatus.ERROR;
    } catch (IOException ex) {
      LOG.debug("Failed to send analytics report: " + ex.getMessage());
      return SentStatus.ERROR;
    }
  }
}
