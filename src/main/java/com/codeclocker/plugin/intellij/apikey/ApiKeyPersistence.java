package com.codeclocker.plugin.intellij.apikey;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.intellij.ide.util.PropertiesComponent;
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
      PropertiesComponent.getInstance().setValue(CODE_CLOCKER_API_KEY_PROPERTY, null);
    } else {
      PropertiesComponent.getInstance().setValue(CODE_CLOCKER_API_KEY_PROPERTY, apiKey);
    }
  }

  public static void unsetApiKey() {
    PropertiesComponent.getInstance().setValue(CODE_CLOCKER_API_KEY_PROPERTY, null);
  }
}
