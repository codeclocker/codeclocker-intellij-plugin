package com.codeclocker.plugin.intellij.config;

import static com.codeclocker.plugin.intellij.HubHost.HUB_API_HOST;
import static com.codeclocker.plugin.intellij.JsonMapper.OBJECT_MAPPER;
import static com.codeclocker.plugin.intellij.Timeouts.CONNECT_TIMEOUT;
import static com.codeclocker.plugin.intellij.Timeouts.READ_TIMEOUT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.Nullable;

public class PluginConfigHttpClient {

  private static final Logger LOG = Logger.getInstance(PluginConfigHttpClient.class);

  @Nullable
  public PluginConfigDto getConfig(String apiKey) {
    LOG.debug("Getting plugin config with API Key: {}", apiKey);
    try {
      String response =
          HttpRequests.request(HUB_API_HOST + "/api/v1/plugin-configs")
              .connectTimeout(CONNECT_TIMEOUT)
              .readTimeout(READ_TIMEOUT)
              .tuner(
                  connection -> {
                    if (isNotBlank(apiKey)) {
                      connection.setRequestProperty("X-codeclocker-api-key", apiKey);
                    }
                  })
              .readString();

      return OBJECT_MAPPER.readValue(response, PluginConfigDto.class);
    } catch (Exception ex) {
      LOG.debug("Error getting plugin config: " + ex.getMessage());
    }

    return null;
  }
}
