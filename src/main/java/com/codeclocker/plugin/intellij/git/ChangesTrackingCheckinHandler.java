package com.codeclocker.plugin.intellij.git;

import com.codeclocker.plugin.intellij.git.LineDifferenceCalculator.LineDifferenceResult;
import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.services.BranchActivityTracker;
import com.codeclocker.plugin.intellij.services.CommitActivityTracker;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import java.util.Collection;
import java.util.List;
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

    // Record commit details
    recordCommitDetails(project, changes.size());
  }

  private void recordCommitDetails(Project project, int changedFilesCount) {
    try {
      CommitActivityTracker commitTracker =
          ApplicationManager.getApplication().getService(CommitActivityTracker.class);
      BranchActivityTracker branchTracker =
          ApplicationManager.getApplication().getService(BranchActivityTracker.class);

      if (commitTracker == null) {
        return;
      }

      GitRepositoryManager gitManager = GitRepositoryManager.getInstance(project);
      if (gitManager == null) {
        return;
      }

      List<GitRepository> repos = gitManager.getRepositories();
      if (repos.isEmpty()) {
        return;
      }

      GitRepository repo = repos.get(0);
      String hash = getLatestCommitHash(project, repo);
      String author = getGitAuthor(project, repo);
      String message = panel.getCommitMessage();
      String branch =
          branchTracker != null ? branchTracker.getCurrentBranch(project.getName()) : null;

      // Truncate message to first line
      if (message != null && message.contains("\n")) {
        message = message.substring(0, message.indexOf("\n"));
      }

      CommitRecord record =
          new CommitRecord(
              hash != null ? hash : "unknown",
              message != null ? message : "",
              author != null ? author : "unknown",
              System.currentTimeMillis(),
              changedFilesCount,
              branch != null ? branch : "unknown");

      commitTracker.recordCommit(project.getName(), record);
    } catch (Exception e) {
      LOG.warn("Failed to record commit details", e);
    }
  }

  @Nullable
  private String getLatestCommitHash(Project project, GitRepository repo) {
    try {
      GitLineHandler handler = new GitLineHandler(project, repo.getRoot(), GitCommand.REV_PARSE);
      handler.addParameters("--short", "HEAD");
      GitCommandResult result = Git.getInstance().runCommand(handler);
      if (result.success()) {
        List<String> output = result.getOutput();
        return output.isEmpty() ? null : output.get(0).trim();
      }
    } catch (Exception e) {
      LOG.debug("Failed to get commit hash", e);
    }
    return null;
  }

  @Nullable
  private String getGitAuthor(Project project, GitRepository repo) {
    try {
      GitLineHandler handler = new GitLineHandler(project, repo.getRoot(), GitCommand.CONFIG);
      handler.addParameters("user.name");
      GitCommandResult result = Git.getInstance().runCommand(handler);
      if (result.success()) {
        List<String> output = result.getOutput();
        return output.isEmpty() ? null : output.get(0).trim();
      }
    } catch (Exception e) {
      LOG.debug("Failed to get git author", e);
    }
    return null;
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
