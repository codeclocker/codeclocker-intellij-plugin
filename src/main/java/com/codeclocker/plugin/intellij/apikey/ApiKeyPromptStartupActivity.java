package com.codeclocker.plugin.intellij.apikey;

import com.intellij.openapi.application.ApplicationManager;

public class ApiKeyPromptStartupActivity {

  public static void showApiKeyDialog() {
    if (ApiKeyPersistence.getApiKey() == null) {
      ApplicationManager.getApplication().invokeLater(EnterApiKeyAction::showAction);
    }
  }
}
