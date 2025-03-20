package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class CodeTrackingListener implements DocumentListener {

  private final ChangesActivityTracker tracker;

  public CodeTrackingListener() {
    this.tracker = ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent event) {
    Document document = event.getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) {
      return;
    }

    Project project = ProjectLocator.getInstance().guessProjectForFile(file);
    if (project == null) {
      return;
    }

    if (isGitIgnored(project, file)) {
      return;
    }

    String filePath = getRelativePath(project, file);
    String oldText = event.getOldFragment().toString();
    String newText = event.getNewFragment().toString();

    int oldLines = countLines(oldText);
    int newLines = countLines(newText);

    int diff = newLines - oldLines;
    if (diff > 0) {
      tracker.incrementAdditions(project.getName(), filePath, file.getExtension(), newLines);
    }
    if (diff < 0) {
      tracker.incrementRemovals(project.getName(), filePath, file.getExtension(), oldLines);
    }
  }

  private int countLines(String text) {
    if (text.isEmpty()) {
      return 0;
    }

    return StringUtils.countMatches(text, "\n");
  }

  public boolean isGitIgnored(Project project, VirtualFile file) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    return changeListManager.isIgnoredFile(file);
  }

  private static String getRelativePath(Project project, VirtualFile file) {
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
}
