package com.codeclocker.plugin.intellij.tracking;

import com.intellij.ide.util.PropertiesComponent;

/** Handles persistent storage of tracking behavior settings. */
public class TrackingPersistence {

  private static final String PAUSE_ON_FOCUS_LOST = "com.codeclocker.tracking.pause-on-focus-lost";
  private static final String INACTIVITY_TIMEOUT_SECONDS =
      "com.codeclocker.tracking.inactivity-timeout-seconds";

  private static final boolean DEFAULT_PAUSE_ON_FOCUS_LOST = true;
  private static final int DEFAULT_INACTIVITY_TIMEOUT_SECONDS = 120; // 2 minutes

  /**
   * Check if tracking should pause when IDE loses focus.
   *
   * @return true if tracking should pause on focus lost (default: true)
   */
  public static boolean isPauseOnFocusLostEnabled() {
    return PropertiesComponent.getInstance()
        .getBoolean(PAUSE_ON_FOCUS_LOST, DEFAULT_PAUSE_ON_FOCUS_LOST);
  }

  /**
   * Enable or disable pause on focus lost.
   *
   * @param enabled true to pause tracking when IDE loses focus
   */
  public static void setPauseOnFocusLostEnabled(boolean enabled) {
    PropertiesComponent.getInstance()
        .setValue(PAUSE_ON_FOCUS_LOST, enabled, DEFAULT_PAUSE_ON_FOCUS_LOST);
  }

  /**
   * Get the inactivity timeout in seconds.
   *
   * @return timeout in seconds (default: 120)
   */
  public static int getInactivityTimeoutSeconds() {
    return PropertiesComponent.getInstance()
        .getInt(INACTIVITY_TIMEOUT_SECONDS, DEFAULT_INACTIVITY_TIMEOUT_SECONDS);
  }

  /**
   * Set the inactivity timeout.
   *
   * @param seconds timeout in seconds
   */
  public static void setInactivityTimeoutSeconds(int seconds) {
    PropertiesComponent.getInstance()
        .setValue(INACTIVITY_TIMEOUT_SECONDS, seconds, DEFAULT_INACTIVITY_TIMEOUT_SECONDS);
  }
}
