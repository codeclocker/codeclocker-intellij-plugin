package com.codeclocker.plugin.intellij.analytics;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Manages a unique installation ID for anonymous analytics tracking. The ID is generated once and
 * persisted across IDE sessions.
 */
public class InstallationIdPersistence {

  private static final Logger LOG = Logger.getInstance(InstallationIdPersistence.class);

  private static final String INSTALLATION_ID_PROPERTY = "com.codeclocker.installation-id";

  /**
   * Gets the installation ID, generating one if it doesn't exist.
   *
   * @return The unique installation ID for this IDE installation
   */
  @NotNull
  public static String getInstallationId() {
    PropertiesComponent properties = PropertiesComponent.getInstance();
    String installationId = properties.getValue(INSTALLATION_ID_PROPERTY);

    if (installationId == null || installationId.isBlank()) {
      installationId = generateInstallationId();
      properties.setValue(INSTALLATION_ID_PROPERTY, installationId);
      LOG.info("Generated new installation ID: " + installationId);
    }

    return installationId;
  }

  private static String generateInstallationId() {
    return UUID.randomUUID().toString();
  }
}
