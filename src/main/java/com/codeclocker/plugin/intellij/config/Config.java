package com.codeclocker.plugin.intellij.config;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {

  private static final Logger LOG = Logger.getInstance(Config.class);
  private static final Properties CONFIG = new Properties();

  static {
    try (InputStream input =
        Config.class.getClassLoader().getResourceAsStream("config.properties")) {
      if (input == null) {
        LOG.warn("config.properties file not found. Using default values.");
      } else {
        CONFIG.load(input);
        LOG.info("Successfully loaded config.properties");
      }
    } catch (IOException ex) {
      LOG.error("Error loading config.properties", ex);
    }
  }

  private Config() {
    // Utility class - prevent instantiation
  }

  /**
   * When enabled, validates that the global stopwatch time matches the sum of all per-project times
   * before each data flush. This helps detect timing inconsistencies, data corruption, or race
   * conditions.
   *
   * <p>Default: false (disabled)
   *
   * @return true if timer validation is enabled, false otherwise
   */
  public static boolean isValidateTimersEnabled() {
    return Boolean.parseBoolean(CONFIG.getProperty("feature.validateTimers.enabled", "false"));
  }
}
