package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.TimeSpentActivityTracker;
import com.codeclocker.plugin.intellij.tracking.TrackingPersistence;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.NotNull;

public class AppFrameFocusLostListener implements ApplicationActivationListener {

  private static final Logger LOG = Logger.getInstance(AppFrameFocusLostListener.class);

  private final TimeSpentActivityTracker tracker;

  public AppFrameFocusLostListener() {
    this.tracker = ApplicationManager.getApplication().getService(TimeSpentActivityTracker.class);
  }

  @Override
  public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
    if (!TrackingPersistence.isPauseOnFocusLostEnabled()) {
      LOG.debug("Application frame lost focus, but pause on focus lost is disabled");
      return;
    }
    LOG.debug("Application frame lost focus. Pausing all activity tracking");
    tracker.pause();
  }
}
