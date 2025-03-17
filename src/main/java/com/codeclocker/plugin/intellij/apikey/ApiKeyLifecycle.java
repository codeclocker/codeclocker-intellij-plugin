package com.codeclocker.plugin.intellij.apikey;

import static com.codeclocker.plugin.intellij.Notifications.SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.Notifications;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class ApiKeyLifecycle {

  private static final Logger LOG = Logger.getInstance(ApiKeyPersistence.class);

  public static final String ACTIVITY_DATA_STOPPED_BEING_COLLECTED_PROPERTY =
      "com.codeclocker.activity-data-stopped-being-collected";

  public void processHubErrorResponse(@Nullable String response) {
    if (response == null) {
      return;
    }

    if (StringUtils.contains(response, "Unknown API key")) {
      ApiKeyPersistence.unsetApiKey();
      Notifications.showInvalidApiKeyNotification();
    } else if (StringUtils.contains(response, "Subscription expire soon")) {
      // todo: show notification
    } else if (StringUtils.contains(response, "Subscription expired")) {
      Notifications.showPaymentExpiredNotification();
    } else if (StringUtils.contains(response, "Activity data stopped being collected")) {
      PropertiesComponent.getInstance()
          .setValue(ACTIVITY_DATA_STOPPED_BEING_COLLECTED_PROPERTY, true);
    }
  }

  public static boolean isActivityDataStoppedBeingCollected() {
    return PropertiesComponent.getInstance()
        .getBoolean(ACTIVITY_DATA_STOPPED_BEING_COLLECTED_PROPERTY, false);
  }

  public static void continueCollectingActivityData() {
    LOG.debug("Continuing collecting data by API Key");
    PropertiesComponent properties = PropertiesComponent.getInstance();
    properties.setValue(ACTIVITY_DATA_STOPPED_BEING_COLLECTED_PROPERTY, false);
    properties.setValue(SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN, false);
  }

  @Nullable
  public static String getActiveApiKey() {
    String apiKey = ApiKeyPersistence.getApiKey();
    if (isBlank(apiKey)) {
      LOG.debug("API Key is not set");
      return null;
    }

    boolean activitiDataStoppedBeingCollected =
        PropertiesComponent.getInstance()
            .getBoolean(ACTIVITY_DATA_STOPPED_BEING_COLLECTED_PROPERTY, false);
    if (activitiDataStoppedBeingCollected) {
      LOG.warn("Activity data stopped being collected");
      return null;
    }

    return apiKey;
  }
}
