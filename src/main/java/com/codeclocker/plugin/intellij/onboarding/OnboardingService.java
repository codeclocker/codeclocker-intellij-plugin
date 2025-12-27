package com.codeclocker.plugin.intellij.onboarding;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.concurrent.atomic.AtomicBoolean;

/** Service managing the onboarding tutorial flow. */
@Service(Service.Level.APP)
public final class OnboardingService {

  private static final Logger LOG = Logger.getInstance(OnboardingService.class);

  private final AtomicBoolean tutorialInProgress = new AtomicBoolean(false);
  private Project currentProject;

  public static OnboardingService getInstance() {
    return ApplicationManager.getApplication().getService(OnboardingService.class);
  }

  /**
   * Start the onboarding tutorial for a new user. Called when the plugin is first installed or when
   * the user requests to restart the tutorial.
   */
  public void startOnboarding(Project project) {
    if (!OnboardingPersistence.shouldShowOnboarding()) {
      LOG.debug("Onboarding already completed or skipped");
      return;
    }

    if (tutorialInProgress.getAndSet(true)) {
      LOG.debug("Onboarding already in progress");
      return;
    }

    this.currentProject = project;
    OnboardingPersistence.setCurrentStep(OnboardingStep.WELCOME);

    // Show the first step
    showCurrentStep();
  }

  /** Resume onboarding from where the user left off. */
  public void resumeOnboarding(Project project) {
    if (!OnboardingPersistence.shouldShowOnboarding()) {
      return;
    }

    if (tutorialInProgress.get()) {
      return;
    }

    this.currentProject = project;
    tutorialInProgress.set(true);

    // Resume from current step
    showCurrentStep();
  }

  /** Advance to the next step in the tutorial. */
  public void nextStep() {
    OnboardingStep current = OnboardingPersistence.getCurrentStep();
    OnboardingStep next = current.next();

    if (next == OnboardingStep.COMPLETED) {
      completeOnboarding();
    } else {
      OnboardingPersistence.setCurrentStep(next);
      showCurrentStep();
    }
  }

  /** Skip the entire onboarding tutorial. */
  public void skipOnboarding() {
    OnboardingPersistence.setOnboardingSkipped(true);
    tutorialInProgress.set(false);
    LOG.info("Onboarding tutorial skipped");
  }

  /** Mark the onboarding as completed. */
  public void completeOnboarding() {
    OnboardingPersistence.setOnboardingCompleted(true);
    OnboardingPersistence.setCurrentStep(OnboardingStep.COMPLETED);
    tutorialInProgress.set(false);
    LOG.info("Onboarding tutorial completed");

    // Show completion message
    if (currentProject != null) {
      OnboardingStepRenderer.showCompletionNotification(currentProject);
    }
  }

  /** Reset and restart the onboarding tutorial. */
  public void restartOnboarding(Project project) {
    OnboardingPersistence.resetOnboarding();
    tutorialInProgress.set(false);
    startOnboarding(project);
  }

  /** Show the current step of the tutorial. */
  private void showCurrentStep() {
    OnboardingStep step = OnboardingPersistence.getCurrentStep();

    if (currentProject == null || currentProject.isDisposed()) {
      LOG.warn("Cannot show onboarding step: project is null or disposed");
      return;
    }

    LOG.debug("Showing onboarding step: " + step.name());

    ApplicationManager.getApplication()
        .invokeLater(() -> OnboardingStepRenderer.renderStep(step, currentProject, this));
  }

  public boolean isTutorialInProgress() {
    return tutorialInProgress.get();
  }

  public Project getCurrentProject() {
    return currentProject;
  }
}
