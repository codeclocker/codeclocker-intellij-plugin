package com.codeclocker.plugin.intellij.apikey;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;

public class ApiKeyPromptStartupActivity {

  private static final String API_KEY_PROMPT_SHOWN_PROPERTY =
      "com.codeclocker.api-key-prompt-shown";

  public static void showApiKeyDialog() {
    if (ApiKeyPersistence.getApiKey() == null && !wasPromptAlreadyShown()) {
      markPromptAsShown();
      ApplicationManager.getApplication().invokeLater(EnterApiKeyAction::showAction);
    }
  }

  private static boolean wasPromptAlreadyShown() {
    return PropertiesComponent.getInstance().getBoolean(API_KEY_PROMPT_SHOWN_PROPERTY, false);
  }

  private static void markPromptAsShown() {
    PropertiesComponent.getInstance().setValue(API_KEY_PROMPT_SHOWN_PROPERTY, true);
  }
}
