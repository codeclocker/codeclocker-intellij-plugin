package com.codeclocker.plugin.intellij.git;

import com.codeclocker.plugin.intellij.services.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class GitCommitStatsListener extends CheckinHandlerFactory {

  private final ChangesActivityTracker tracker;

  public GitCommitStatsListener() {
    this.tracker = ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
  }

  @NotNull
  @Override
  public CheckinHandler createHandler(
      @NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
    return new ChangesTrackingCheckinHandler(tracker, panel);
  }
}
