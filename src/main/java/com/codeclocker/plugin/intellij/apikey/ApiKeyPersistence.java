package com.codeclocker.plugin.intellij.apikey;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.reporting.DataReportingTask;
import com.codeclocker.plugin.intellij.reporting.TimeComparisonFetchTask;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

public class ApiKeyPersistence {

  private static final Logger LOG = Logger.getInstance(ApiKeyPersistence.class);

  private static final String CODE_CLOCKER_API_KEY_PROPERTY = "com.codeclocker.api-key";

  @Nullable
  public static String getApiKey() {
    String apiKey = PropertiesComponent.getInstance().getValue(CODE_CLOCKER_API_KEY_PROPERTY);
    LOG.debug("Retrieved API Key: {}", apiKey);

    return apiKey;
  }

  public static void persistApiKey(String apiKey) {
    if (isBlank(apiKey)) {
      unsetApiKey();
    } else {
      PropertiesComponent.getInstance().setValue(CODE_CLOCKER_API_KEY_PROPERTY, apiKey);
      // Sync any locally stored data to the server now that we have an API key
      syncLocalDataToServer(apiKey);
    }
  }

  private static void syncLocalDataToServer(String apiKey) {
    try {
      DataReportingTask dataReportingTask =
          ApplicationManager.getApplication().getService(DataReportingTask.class);
      if (dataReportingTask != null) {
        dataReportingTask.syncLocalDataToServer(apiKey);
      }

      // Refetch trends data now that local data has been synced
      TimeComparisonFetchTask timeComparisonFetchTask =
          ApplicationManager.getApplication().getService(TimeComparisonFetchTask.class);
      if (timeComparisonFetchTask != null) {
        timeComparisonFetchTask.refetch();
      }
    } catch (Exception e) {
      LOG.warn("Failed to sync local data after API key was set", e);
    }
  }

  public static void unsetApiKey() {
    PropertiesComponent.getInstance().setValue(CODE_CLOCKER_API_KEY_PROPERTY, null);
  }
}
