package com.codeclocker.plugin.intellij.onboarding;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/** Action to restart the onboarding tutorial from the Tools menu. */
public class RestartOnboardingAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      OnboardingService.getInstance().restartOnboarding(project);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    // Always available when there's a project
    e.getPresentation().setEnabledAndVisible(e.getProject() != null);
  }
}
