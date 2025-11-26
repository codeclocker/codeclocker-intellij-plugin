package com.codeclocker.plugin.intellij.git;

import com.codeclocker.plugin.intellij.git.LineDifferenceCalculator.LineDifferenceResult;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

public class ChangesTrackingCheckinHandler extends CheckinHandler {

  private static final Logger LOG = Logger.getInstance(ChangesTrackingCheckinHandler.class);

  private final ChangesActivityTracker tracker;
  private final CheckinProjectPanel panel;

  public ChangesTrackingCheckinHandler(ChangesActivityTracker tracker, CheckinProjectPanel panel) {
    this.tracker = tracker;
    this.panel = panel;
  }

  @Override
  public void checkinSuccessful() {
    Project project = panel.getProject();
    Collection<Change> changes = panel.getSelectedChanges();

    for (Change change : changes) {
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();

      try {
        String relativePath = getRelativePath(project, beforeRevision, afterRevision);
        if (relativePath == null) {
          continue;
        }

        LineDifferenceResult diff =
            LineDifferenceCalculator.calculateLineDifferences(
                beforeRevision == null ? null : beforeRevision.getContent(),
                afterRevision == null ? null : afterRevision.getContent());

        String extension = getExtension(relativePath);

        if (diff.addedLines() > 0) {
          tracker.incrementAdditions(project.getName(), relativePath, extension, diff.addedLines());
        }
        if (diff.removedLines() > 0) {
          tracker.incrementRemovals(
              project.getName(), relativePath, extension, diff.removedLines());
        }
      } catch (Exception ex) {
        LOG.debug("Error handling checking event: {}", ex.getMessage());
      }
    }
  }

  private String getExtension(String relativePath) {
    int lastDotIndex = relativePath.lastIndexOf('.');

    if (lastDotIndex == -1 || lastDotIndex == relativePath.length() - 1) {
      return "";
    }

    return relativePath.substring(lastDotIndex + 1);
  }

  @Nullable
  private static String getRelativePath(
      Project project, ContentRevision beforeRevision, ContentRevision afterRevision) {
    FilePath file = getFile(beforeRevision, afterRevision);
    if (file == null) {
      return null;
    }

    String projectBasePath = project.getBasePath();
    String filePath = file.getPath();

    if (projectBasePath == null) {
      return filePath;
    }

    if (filePath.startsWith(projectBasePath)) {
      return filePath.substring(projectBasePath.length());
    }

    return filePath;
  }

  @Nullable
  private static FilePath getFile(ContentRevision beforeRevision, ContentRevision afterRevision) {
    if (afterRevision != null) {
      return afterRevision.getFile();
    } else if (beforeRevision != null) {
      return beforeRevision.getFile();
    }

    return null;
  }
}
