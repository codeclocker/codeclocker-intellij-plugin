package com.codeclocker.plugin.intellij.git;

import com.codeclocker.plugin.intellij.services.BranchActivityTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for git repository changes (including branch switches) and notifies
 * BranchActivityTracker.
 */
public class BranchChangeListener implements GitRepositoryChangeListener {

  @Override
  public void repositoryChanged(@NotNull GitRepository repository) {
    Project project = repository.getProject();
    if (project.isDisposed()) {
      return;
    }

    String branchName = getBranchName(repository);
    String projectName = project.getName();

    BranchActivityTracker tracker =
        ApplicationManager.getApplication().getService(BranchActivityTracker.class);
    if (tracker != null) {
      tracker.onBranchChange(projectName, branchName);
    }
  }

  private String getBranchName(GitRepository repo) {
    if (repo.getCurrentBranch() != null) {
      return repo.getCurrentBranch().getName();
    }
    // Detached HEAD state - show short hash
    String revision = repo.getCurrentRevision();
    if (revision != null && revision.length() > 7) {
      return "detached:" + revision.substring(0, 7);
    }
    return "detached";
  }
}
