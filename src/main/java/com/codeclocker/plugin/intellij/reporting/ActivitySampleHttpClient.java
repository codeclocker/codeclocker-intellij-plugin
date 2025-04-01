package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.ERROR;
import static com.codeclocker.plugin.intellij.reporting.SentStatus.OK;
import static com.intellij.util.io.HttpRequests.JSON_CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import java.io.IOException;

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
      String response =
          HttpRequests.post(HUB_API_HOST + path, JSON_CONTENT_TYPE)
              .connectTimeout(CONNECT_TIMEOUT)
              .readTimeout(READ_TIMEOUT)
              .tuner(connection -> connection.setRequestProperty("X-codeclocker-api-key", apiKey))
              .connect(
                  request -> {
                    request.write(jsonData.getBytes(UTF_8));
                    return request.readString();
                  });

      apiKeyLifecycle.processHubErrorResponse(response);

      return OK;
    } catch (IOException ex) {
      LOG.debug("Error sending activity sample: " + ex.getMessage());
      return ERROR;
    }
  }
}
