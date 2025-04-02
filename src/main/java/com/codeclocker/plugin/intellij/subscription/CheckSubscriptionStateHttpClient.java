package com.codeclocker.plugin.intellij.subscription;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.Nullable;

public class CheckSubscriptionStateHttpClient {

  private static final Logger LOG = Logger.getInstance(CheckSubscriptionStateHttpClient.class);

  @Nullable
  public String check(String apiKey) {
    LOG.debug("Checking subscription state by API Key: {}", apiKey);
    try {
      return HttpRequests.request(HUB_API_HOST + "/api/v1/subscriptions/state-by-api-key")
          .connectTimeout(CONNECT_TIMEOUT)
          .readTimeout(READ_TIMEOUT)
          .tuner(connection -> connection.setRequestProperty("X-codeclocker-api-key", apiKey))
          .readString();
    } catch (Exception ex) {
      LOG.debug("Error checking subscription state: {}", ex.getMessage());
      return null;
    }
  }
}
