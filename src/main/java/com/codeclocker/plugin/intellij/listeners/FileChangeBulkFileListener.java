package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class FileChangeBulkFileListener implements BulkFileListener {

  private final ChangesActivityTracker tracker;

  public FileChangeBulkFileListener() {
    this.tracker = ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    for (VFileEvent e : events) {
      if (!(e instanceof VFileContentChangeEvent event)) {
        continue;
      }
      VirtualFile file = event.getFile();

      Project project = ProjectLocator.getInstance().guessProjectForFile(file);
      if (project == null) {
        return;
      }

      if (isGitIgnored(project, file)) {
        return;
      }

      String filePath = getRelativePath(project, file);
      long change = event.getNewLength() - event.getOldLength();
      if (change > 0) {
        tracker.incrementAdditions(project.getName(), filePath, file.getExtension(), change);
      }
      if (change < 0) {
        tracker.incrementRemovals(
            project.getName(), filePath, file.getExtension(), Math.abs(change));
      }
    }
  }

  public static String getRelativePath(Project project, VirtualFile file) {
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

  public boolean isGitIgnored(Project project, VirtualFile file) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    return changeListManager.isIgnoredFile(file);
  }
}
