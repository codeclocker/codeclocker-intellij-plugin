package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.ActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class FileChangeBulkFileListener implements BulkFileListener {

  private final ActivityTracker activityTracker;

  public FileChangeBulkFileListener() {
    this.activityTracker = ApplicationManager.getApplication().getService(ActivityTracker.class);
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    for (VFileEvent e : events) {
      if (!(e instanceof VFileContentChangeEvent event)) {
        continue;
      }
      VirtualFile file = event.getFile();

      Project project = getProjectForFile(file);
      if (project == null) {
        return;
      }

      if (isGitIgnored(project, file)) {
        return;
      }

      Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
      if (module == null) {
        return;
      }

      long change = event.getNewLength() - event.getOldLength();
      if (change > 0) {
        activityTracker.logAdditions(project, module, file, change);
      }
      if (change < 0) {
        activityTracker.logRemovals(project, module, file, Math.abs(change));
      }
    }
  }

  public static Project getProjectForFile(VirtualFile file) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
        return project;
      }
    }
    return null;
  }

  public boolean isGitIgnored(Project project, VirtualFile file) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    return changeListManager.isIgnoredFile(file);
  }
}
