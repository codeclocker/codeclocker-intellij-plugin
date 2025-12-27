package com.codeclocker.plugin.intellij.onboarding;

import com.intellij.ide.util.PropertiesComponent;

/** Persistence for onboarding tutorial state. */
public final class OnboardingPersistence {

  private static final String ONBOARDING_COMPLETED_KEY = "com.codeclocker.onboarding.completed";
  private static final String ONBOARDING_CURRENT_STEP_KEY =
      "com.codeclocker.onboarding.currentStep";
  private static final String ONBOARDING_SKIPPED_KEY = "com.codeclocker.onboarding.skipped";

  private OnboardingPersistence() {}

  public static boolean isOnboardingCompleted() {
    return PropertiesComponent.getInstance().getBoolean(ONBOARDING_COMPLETED_KEY, false);
  }

  public static void setOnboardingCompleted(boolean completed) {
    PropertiesComponent.getInstance().setValue(ONBOARDING_COMPLETED_KEY, completed);
  }

  public static boolean isOnboardingSkipped() {
    return PropertiesComponent.getInstance().getBoolean(ONBOARDING_SKIPPED_KEY, false);
  }

  public static void setOnboardingSkipped(boolean skipped) {
    PropertiesComponent.getInstance().setValue(ONBOARDING_SKIPPED_KEY, skipped);
  }

  public static OnboardingStep getCurrentStep() {
    String stepName =
        PropertiesComponent.getInstance()
            .getValue(ONBOARDING_CURRENT_STEP_KEY, OnboardingStep.WELCOME.name());
    try {
      return OnboardingStep.valueOf(stepName);
    } catch (IllegalArgumentException e) {
      return OnboardingStep.WELCOME;
    }
  }

  public static void setCurrentStep(OnboardingStep step) {
    PropertiesComponent.getInstance().setValue(ONBOARDING_CURRENT_STEP_KEY, step.name());
  }

  public static void resetOnboarding() {
    PropertiesComponent.getInstance().unsetValue(ONBOARDING_COMPLETED_KEY);
    PropertiesComponent.getInstance().unsetValue(ONBOARDING_CURRENT_STEP_KEY);
    PropertiesComponent.getInstance().unsetValue(ONBOARDING_SKIPPED_KEY);
  }

  /** Check if onboarding should be shown (not completed and not skipped). */
  public static boolean shouldShowOnboarding() {
    return !isOnboardingCompleted() && !isOnboardingSkipped();
  }
}
