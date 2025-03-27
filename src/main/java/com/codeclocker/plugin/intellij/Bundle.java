package com.codeclocker.plugin.intellij;

import com.intellij.DynamicBundle;
import java.util.function.Supplier;
import org.jetbrains.annotations.PropertyKey;

public class Bundle extends DynamicBundle {

  private static final String BUNDLE = "messages.Bundle";
  private static final Bundle INSTANCE = new Bundle();

  private Bundle() {
    super(BUNDLE);
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }

  @SuppressWarnings("unused")
  public static Supplier<String> messagePointer(
      @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getLazyMessage(key, params);
  }
}
