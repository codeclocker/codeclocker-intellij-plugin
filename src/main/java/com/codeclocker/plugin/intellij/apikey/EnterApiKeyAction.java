package com.codeclocker.plugin.intellij.apikey;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codeclocker.plugin.intellij.subscription.SubscriptionStateCheckerTask;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class EnterApiKeyAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    showAction();
  }

  public static void showAction() {
    String text = getText();
    int result =
        Messages.showOkCancelDialog(
            text,
            "Enter CodeClocker API Key",
            "Get API Key",
            "Cancel",
            Messages.getInformationIcon());
    if (result == Messages.YES) {
      BrowserUtil.browse(HUB_UI_HOST + "/api-key");
      showApiKeyInputDialog();
    }
  }

  private static String getText() {
    String apiKey = ApiKeyPersistence.getApiKey();
    if (isBlank(apiKey)) {
      return """
          Enter your CodeClocker API Key to start tracking your coding activity.""";
    }

    return """
        Your current API key: %s

        Enter your CodeClocker API Key to start tracking your coding activity."""
        .formatted(apiKey);
  }

  private static void showApiKeyInputDialog() {
    String apiKey =
        Messages.showInputDialog(
            "Enter your API key\n\nCopy it from: hub.codeclocker.com/api-key",
            "Activate CodeClocker",
            Messages.getInformationIcon(),
            null,
            new ApiKeyInputValidator());
    ApiKeyPersistence.persistApiKey(apiKey);
    ApplicationManager.getApplication().getService(SubscriptionStateCheckerTask.class).schedule();
  }
}
