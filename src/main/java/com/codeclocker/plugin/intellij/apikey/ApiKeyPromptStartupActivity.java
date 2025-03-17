package com.codeclocker.plugin.intellij.apikey;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;

public class ApiKeyPromptStartupActivity {

  private static final String FIRST_LAUNCH_PROPERTY = "com.codeclocker.first-launch";

  public static void showApiKeyDialog() {
    PropertiesComponent properties = PropertiesComponent.getInstance();
    boolean prompt = properties.getBoolean("com.codeclocker.prompt-for-api-key", true);

    if (ApiKeyPersistence.getApiKey() == null && prompt) {
      ApplicationManager.getApplication()
          .invokeLater(
              () -> {
                EnterApiKeyAction.showAction();
                properties.setValue(FIRST_LAUNCH_PROPERTY, false);
              });
    }
  }
}
