package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static com.codeclocker.plugin.intellij.http.HttpResponseReader.readResponse;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class ActivitySampleHttpClient {

  private static final Logger LOG = Logger.getInstance(ActivitySampleHttpClient.class);

  private final ApiKeyLifecycle apiKeyLifecycle;

  public ActivitySampleHttpClient() {
    this.apiKeyLifecycle = ApplicationManager.getApplication().getService(ApiKeyLifecycle.class);
  }

  public SentStatus sendTimeSpentSample(String apiKey, String jsonData) {
    return send("/api/v1/samples/time-spent", apiKey, jsonData);
  }

  public SentStatus sendChangesSample(String apiKey, String jsonData) {
    return send("/api/v1/samples/changes", apiKey, jsonData);
  }

  private SentStatus send(String path, String apiKey, String jsonData) {
    try {
      LOG.debug("Posting data: {}", jsonData);
      HttpURLConnection connection =
          (HttpURLConnection) HttpConfigurable.getInstance().openConnection(HUB_API_HOST + path);
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      connection.setRequestProperty("X-codeclocker-api-key", apiKey);
      connection.setDoOutput(true);

      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonData.getBytes(UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = connection.getResponseCode();
      LOG.debug("HTTP Response Code: " + responseCode);
      processResponse(connection);

      if (responseCode != 200) {
        return ERROR;
      }

      return OK;
    } catch (IOException ex) {
      LOG.error("Error sending activity sample: {}", ex.getMessage());
    }

    return ERROR;
  }

  private void processResponse(HttpURLConnection connection) {
    try {
      String response = readResponse(connection);
      apiKeyLifecycle.processHubErrorResponse(response);
    } catch (Exception ex) {
      LOG.error("Failed to process response: {}", ex.getMessage());
    }
  }
}
