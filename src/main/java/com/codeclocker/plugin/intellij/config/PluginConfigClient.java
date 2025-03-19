package com.codeclocker.plugin.intellij.config;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static com.codeclocker.plugin.intellij.http.HttpResponseReader.readResponse;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import java.net.HttpURLConnection;
import org.jetbrains.annotations.Nullable;

public class PluginConfigClient {

  private static final Logger LOG = Logger.getInstance(PluginConfigClient.class);

  @Nullable
  public PluginConfigDto getConfig(String apiKey) {
    LOG.debug("Getting plugin config with API Key: {}", apiKey);
    try {
      HttpURLConnection connection =
          (HttpURLConnection)
              HttpConfigurable.getInstance()
                  .openConnection(HUB_API_HOST + "/api/v1/plugin-configs");
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("X-codeclocker-api-key", apiKey);
      connection.setDoOutput(true);

      LOG.debug("HTTP Response Code: " + connection.getResponseCode());
      String response = readResponse(connection);
      if (response != null) {
        return OBJECT_MAPPER.readValue(response, PluginConfigDto.class);
      }
    } catch (Exception ex) {
      LOG.error("Error getting plugin config: {}", ex.getMessage());
    }

    return null;
  }
}
