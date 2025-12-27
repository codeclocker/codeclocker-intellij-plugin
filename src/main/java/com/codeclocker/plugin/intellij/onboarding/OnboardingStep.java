package com.codeclocker.plugin.intellij.onboarding;

/** Enum representing each step in the onboarding tutorial. */
public enum OnboardingStep {
  WELCOME(1, "Welcome to CodeClocker"),
  STATUS_BAR_WIDGET(2, "Status Bar Widget"),
  ACTIVITY_POPUP(3, "Activity Dashboard"),
  GOALS(4, "Set Your Goals"),
  HUB_CONNECTION(5, "Connect to Hub (Optional)"),
  COMPLETED(6, "Tutorial Complete");

  private final int order;
  private final String title;

  OnboardingStep(int order, String title) {
    this.order = order;
    this.title = title;
  }

  public int getOrder() {
    return order;
  }

  public String getTitle() {
    return title;
  }

  public OnboardingStep next() {
    for (OnboardingStep step : values()) {
      if (step.order == this.order + 1) {
        return step;
      }
    }
    return COMPLETED;
  }

  public boolean isLast() {
    return this == HUB_CONNECTION;
  }
}
