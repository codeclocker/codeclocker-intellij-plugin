package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class PauseProjectOnProjectClosing implements ProjectManagerListener {

  private static final Logger LOG = Logger.getInstance(PauseProjectOnProjectClosing.class);

  private final TimeSpentPerProjectLogger logger;

  public PauseProjectOnProjectClosing() {
    this.logger = ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
  }

  @Override
  public void projectClosing(@NotNull Project project) {
    LOG.debug("Project closing: " + project.getName());
    logger.pauseProject(project);
  }
}
