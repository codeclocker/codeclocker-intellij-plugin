package com.codeclocker.plugin.intellij.apikey;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static com.codeclocker.plugin.intellij.http.HttpResponseReader.readResponse;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import java.net.HttpURLConnection;
import org.jetbrains.annotations.Nullable;

public class CheckApiKeyStateHttpClient {

  private static final Logger LOG = Logger.getInstance(CheckApiKeyStateHttpClient.class);

  @Nullable
  public String check(String apiKey) {
    LOG.debug("Checking subscription state by API Key: {}", apiKey);
    try {
      HttpURLConnection connection =
          (HttpURLConnection)
              HttpConfigurable.getInstance()
                  .openConnection(HUB_API_HOST + "/api/v1/subscriptions/state-by-api-key");
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("X-codeclocker-api-key", apiKey);
      connection.setDoOutput(true);

      LOG.debug("HTTP Response Code: " + connection.getResponseCode());
      return readResponse(connection);
    } catch (Exception ex) {
      LOG.error("Error checking subscription state: {}", ex.getMessage());
      return null;
    }
  }
}
