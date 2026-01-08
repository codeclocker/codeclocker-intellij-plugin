package com.codeclocker.plugin.intellij.onboarding;

import static com.intellij.notification.NotificationType.INFORMATION;

import com.codeclocker.plugin.intellij.analytics.Analytics;
import com.codeclocker.plugin.intellij.analytics.AnalyticsEventType;
import com.codeclocker.plugin.intellij.apikey.EnterApiKeyAction;
import com.codeclocker.plugin.intellij.goal.GoalSettingsDialog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;

public final class OnboardingStepRenderer {

  private OnboardingStepRenderer() {}

  public static void renderStep(OnboardingStep step, Project project, OnboardingService service) {
    switch (step) {
      case WELCOME -> showWelcomeStep(project, service);
      case STATUS_BAR_WIDGET -> showStatusBarWidgetStep(project, service);
      case ACTIVITY_POPUP -> showActivityPopupStep(project, service);
      case GOALS -> showGoalsStep(project, service);
      case HUB_CONNECTION -> showHubConnectionStep(project, service);
      case COMPLETED -> showCompletionNotification(project);
    }
  }

  private static void showWelcomeStep(Project project, OnboardingService service) {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Welcome to CodeClocker!",
                "<b>CodeClocker</b> is now <b>tracking your coding time</b> automatically.<br><br>"
                    + "Let's take a <b>quick tour</b> to help you get the most out of it.",
                INFORMATION);

    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Start tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_WELCOME_START);
              service.nextStep();
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Skip tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_WELCOME_SKIP);
              service.skipOnboarding();
            }));

    notification.notify(project);
  }

  private static void showStatusBarWidgetStep(Project project, OnboardingService service) {
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    if (statusBar == null) {
      service.nextStep();
      return;
    }

    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Status bar widget",
                "Look at the <b>bottom of your IDE</b> - you'll see the <b>CodeClocker widget</b> in the status bar.<br><br>"
                    + "It shows:<br>"
                    + "• <b>Total</b>: Your total coding time today across all projects<br>"
                    + "• <b>Percent</b>: Your progress toward daily goal<br>"
                    + "• <b>Project</b>: Time spent on the current project<br><br>"
                    + "<b>Click the widget</b> anytime to see more details!",
                INFORMATION);

    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Got it, Next",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_STATUS_BAR_NEXT);
              service.nextStep();
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Skip tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_STATUS_BAR_SKIP);
              service.skipOnboarding();
            }));

    notification.notify(project);
  }

  private static void showActivityPopupStep(Project project, OnboardingService service) {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Activity dashboard",
                "Click the <b>status bar widget</b> to open the <b>Activity Dashboard</b>.<br><br>"
                    + "Here you can see:<br>"
                    + "• <b>Daily and weekly goal</b> progress<br>"
                    + "• <b>Time spent</b> per project<br>"
                    + "• <b>VCS lines</b> added/removed<br>"
                    + "• <b>Comparison</b> with yesterday and last week",
                INFORMATION);

    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Got it, Next",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_ACTIVITY_POPUP_NEXT);
              service.nextStep();
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Skip tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_ACTIVITY_POPUP_SKIP);
              service.skipOnboarding();
            }));

    notification.notify(project);
  }

  private static void showGoalsStep(Project project, OnboardingService service) {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Set your coding goals",
                "Stay motivated by setting <b>daily and weekly coding goals</b>!<br><br>"
                    + "You'll see your <b>progress</b> in the status bar and get <b>notified</b> when you reach your targets.<br><br>"
                    + "Would you like to set your goals now?",
                INFORMATION);

    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Set goals now",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_GOALS_SET);
              ApplicationManager.getApplication()
                  .invokeLater(
                      () -> {
                        GoalSettingsDialog.showDialog();
                        service.nextStep();
                      });
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Maybe later",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_GOALS_LATER);
              service.nextStep();
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Skip tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_GOALS_SKIP);
              service.skipOnboarding();
            }));

    notification.notify(project);
  }

  private static void showHubConnectionStep(Project project, OnboardingService service) {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Connect to CodeClocker Hub (Optional)",
                "Want to <b>visualize your coding stats</b> on the web?<br><br>"
                    + "Connect to <b>CodeClocker Hub</b> to unlock:<br>"
                    + "• <b>Interactive charts</b> and graphs<br>"
                    + "• <b>Historical trend</b> analysis<br>"
                    + "• <b>Team collaboration</b> features<br>"
                    + "• <b>Cross-device syncing</b>",
                INFORMATION);

    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Connect to Hub",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_HUB_CONNECT);
              ApplicationManager.getApplication()
                  .invokeLater(
                      () -> {
                        EnterApiKeyAction.showAction();
                        service.completeOnboarding();
                      });
            }));
    notification.addAction(
        NotificationAction.createSimpleExpiring(
            "Skip, finish tour",
            () -> {
              Analytics.track(AnalyticsEventType.TOUR_HUB_SKIP);
              service.completeOnboarding();
            }));

    notification.notify(project);
  }

  public static void showCompletionNotification(Project project) {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "You're all set!",
                "<b>CodeClocker</b> is now tracking your coding time.<br><br>"
                    + "Quick tips:<br>"
                    + "• Click the <b>status bar widget</b> anytime for details<br>"
                    + "• Set goals from the <b>activity popup</b><br>"
                    + "• Access Hub API key settings via <b>Tools menu</b><br><br>"
                    + "Happy coding!",
                INFORMATION);

    notification.notify(project);
  }
}
