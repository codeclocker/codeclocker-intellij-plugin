package com.codeclocker.plugin.intellij.apikey;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.NlsSafe;

public class ApiKeyInputValidator implements InputValidator {

  @Override
  public boolean checkInput(@NlsSafe String s) {
    return isValid(s);
  }

  @Override
  public boolean canClose(@NlsSafe String s) {
    return isValid(s);
  }

  private static boolean isValid(String apiKey) {
    return apiKey != null && apiKey.startsWith("cc-") && apiKey.length() == 39;
  }
}
