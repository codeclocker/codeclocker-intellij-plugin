package com.codeclocker.plugin.intellij.config;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ConfigProvider {

  private static final Logger LOG = Logger.getInstance(ConfigProvider.class);

  private static final String ACTIVITY_DATA_FLUSH_FREQUENCY_SECONDS =
      "com.codeclocker.config.activity-data-flush-frequency-seconds";
  private static final String CHECK_API_KEY_STATE_FREQUENCY_SECONDS =
      "com.codeclocker.config.check-api-key-status-frequency-seconds";
  private static final String NEXT_CONFIG_LOAD_TIMESTAMP =
      "com.codeclocker.config.next-config-load-timestamp";

  private final PluginConfigHttpClient pluginConfigHttpClient;

  public ConfigProvider() {
    this.pluginConfigHttpClient =
        ApplicationManager.getApplication().getService(PluginConfigHttpClient.class);
    long nextConfigLoadTimestamp =
        PropertiesComponent.getInstance().getLong(NEXT_CONFIG_LOAD_TIMESTAMP, -1);
    long now = System.currentTimeMillis();
    if (now > nextConfigLoadTimestamp) {
      LOG.debug(
          "Scheduling config load since now is [{}] and nextConfigLoadTimestamp is [{}]",
          now,
          nextConfigLoadTimestamp);
      EXECUTOR.schedule(this::loadConfig, 5, SECONDS);
    }
  }

  public int getActivityDataFlushFrequencySeconds() {
    return getProperty(ACTIVITY_DATA_FLUSH_FREQUENCY_SECONDS);
  }

  public int getCheckApiKeyStatusFrequencySeconds() {
    return getProperty(CHECK_API_KEY_STATE_FREQUENCY_SECONDS);
  }

  private static int getProperty(String property) {
    PropertiesComponent properties = PropertiesComponent.getInstance();
    int frequencySeconds = properties.getInt(property, 60);
    LOG.debug("Resolved [{}] value: {}", property, frequencySeconds);

    return Math.max(frequencySeconds, 60);
  }

  private void loadConfig() {
    try {
      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      PluginConfigDto config = pluginConfigHttpClient.getConfig(apiKey);
      if (config == null) {
        LOG.debug("Failed to load plugin config");
        return;
      }

      Duration nextConfigLoadTimestamp =
          Duration.ofMinutes(config.nextConfigLoadAfterLastSuccessfulLoadMinutes())
              .plus(System.currentTimeMillis(), ChronoUnit.MILLIS);

      PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
      propertiesComponent.setValue(
          ACTIVITY_DATA_FLUSH_FREQUENCY_SECONDS,
          String.valueOf(config.activityDataFlushFrequencySeconds()));
      propertiesComponent.setValue(
          CHECK_API_KEY_STATE_FREQUENCY_SECONDS,
          String.valueOf(config.checkApiKeyStateFrequencySeconds()));
      propertiesComponent.setValue(
          NEXT_CONFIG_LOAD_TIMESTAMP, String.valueOf(nextConfigLoadTimestamp.toMillis()));
    } catch (Exception e) {
      LOG.debug("Error loading config from hub: {}", e.getMessage());
    }
  }
}
