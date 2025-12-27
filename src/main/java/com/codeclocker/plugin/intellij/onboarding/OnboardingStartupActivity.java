package com.codeclocker.plugin.intellij.onboarding;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Startup activity that triggers the onboarding tutorial for new users. This runs when a project is
 * opened.
 */
public class OnboardingStartupActivity implements ProjectActivity {

  @Nullable
  @Override
  public Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    // Delay the onboarding slightly to let the IDE fully initialize
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              if (OnboardingPersistence.shouldShowOnboarding()) {
                // Start onboarding for new users
                OnboardingService.getInstance().startOnboarding(project);
              }
            });

    return Unit.INSTANCE;
  }
}
