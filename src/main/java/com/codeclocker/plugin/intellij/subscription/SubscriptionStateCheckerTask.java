package com.codeclocker.plugin.intellij.subscription;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle.continueCollectingActivityData;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyPersistence;
import com.codeclocker.plugin.intellij.config.ConfigProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.concurrent.ScheduledFuture;

public class SubscriptionStateCheckerTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(SubscriptionStateCheckerTask.class);

  private final int checkApiKeyStatusFrequencySeconds;

  private final CheckSubscriptionStateHttpClient checkSubscriptionStateByApiKeyHttpClient;
  private ScheduledFuture<?> task;

  public SubscriptionStateCheckerTask() {
    this.checkSubscriptionStateByApiKeyHttpClient =
        ApplicationManager.getApplication().getService(CheckSubscriptionStateHttpClient.class);
    ConfigProvider configProvider =
        ApplicationManager.getApplication().getService(ConfigProvider.class);
    this.checkApiKeyStatusFrequencySeconds = configProvider.getCheckApiKeyStatusFrequencySeconds();
  }

  public void schedule() {
    if (task != null && !task.isCancelled()) {
      return;
    }

    this.task =
        EXECUTOR.scheduleWithFixedDelay(
            this::checkApiKeyState, 60, checkApiKeyStatusFrequencySeconds, SECONDS);
  }

  private void checkApiKeyState() {
    try {
      String apiKey = ApiKeyPersistence.getApiKey();
      if (isBlank(apiKey)) {
        return;
      }

      String state = checkSubscriptionStateByApiKeyHttpClient.check(apiKey);
      if (state != null && state.contains("ACTIVE")) {
        continueCollectingActivityData();
      } else if (state != null && state.contains("Unknown API key")) {
        cancelTask();
      }
    } catch (Exception ex) {
      LOG.debug("Failed to check API key state: {}", ex.getMessage());
    }
  }

  private void cancelTask() {
    if (task != null) {
      task.cancel(false);
    }
  }

  @Override
  public void dispose() {
    cancelTask();
    EXECUTOR.shutdown();
  }
}
