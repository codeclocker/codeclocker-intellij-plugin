package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker.GLOBAL_REMOVALS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.codeclocker.plugin.intellij.analytics.Analytics;
import com.codeclocker.plugin.intellij.analytics.AnalyticsEventType;
import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.apikey.ApiKeyPersistence;
import com.codeclocker.plugin.intellij.apikey.EnterApiKeyAction;
import com.codeclocker.plugin.intellij.goal.GoalProgress;
import com.codeclocker.plugin.intellij.goal.GoalService;
import com.codeclocker.plugin.intellij.goal.GoalSettingsDialog;
import com.codeclocker.plugin.intellij.goal.ProjectGoalPersistence;
import com.codeclocker.plugin.intellij.goal.ProjectGoalSettingsDialog;
import com.codeclocker.plugin.intellij.local.LocalActivityDataProvider;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.services.vcs.ProjectChangesCounters;
import com.codeclocker.plugin.intellij.tracking.TrackingSettingsDialog;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerPopup {

  private static final String WEB_DASHBOARD = "My Dashboard →";
  private static final String SAVE_HISTORY = "Save my data →";
  private static final String RENEW_SUBSCRIPTION = "Renew subscription to keep data forever →";
  private static final String SET_GOALS = "Set Goals...";
  private static final String SET_PROJECT_GOALS = "Set Project Goals...";
  private static final String AUTO_PAUSE = "Auto-Pause...";
  private static final String ACTIVITY_REPORT = "Activity Report...";

  public static ListPopup create(Project project, String totalTime, String projectTime) {
    ChangesActivityTracker tracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    ProjectChangesCounters projectChanges = tracker.getProjectChanges(project.getName());

    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    GoalService goalService = ApplicationManager.getApplication().getService(GoalService.class);

    List<String> items = new ArrayList<>();

    // Goal progress at the top (always shown)
    if (goalService != null) {
      items.add(formatGoalProgress("Daily", goalService.getDailyProgress()));
      items.add(formatGoalProgress("Weekly", goalService.getWeeklyProgress()));
    }

    // Project-specific goals (only shown when custom goals are enabled)
    String projectName = project.getName();
    boolean hasCustomProjectGoals = ProjectGoalPersistence.hasCustomGoals(projectName);
    if (hasCustomProjectGoals && goalService != null) {
      items.add(
          formatProjectGoalProgress(
              "Daily", projectName, goalService.getProjectDailyProgress(projectName)));
      items.add(
          formatProjectGoalProgress(
              "Weekly", projectName, goalService.getProjectWeeklyProgress(projectName)));
    }

    // Coding time
    items.add("Total: " + totalTime);
    items.add(project.getName() + ": " + projectTime);

    // VCS changes
    items.add("Total: " + getFormattedVcsChanges());
    items.add(project.getName() + ": " + formatProjectVcsChanges(projectChanges));

    // Trends (calculated on-demand from local data)
    items.add(formatTodayVsYesterday(dataProvider));
    items.add(formatThisWeekVsLastWeek(dataProvider));

    // Add settings actions
    items.add(SET_GOALS);
    items.add(SET_PROJECT_GOALS);
    items.add(AUTO_PAUSE);
    items.add(ACTIVITY_REPORT);

    boolean hasApiKey = isNotBlank(ApiKeyPersistence.getApiKey());
    if (ApiKeyLifecycle.isActivityDataStoppedBeingCollected()) {
      items.add(RENEW_SUBSCRIPTION);
    } else if (hasApiKey) {
      items.add(WEB_DASHBOARD);
    } else {
      items.add(SAVE_HISTORY);
    }

    BaseListPopupStep<String> step =
        new BaseListPopupStep<>("Activity", items) {
          @Override
          public boolean isSelectable(String value) {
            return WEB_DASHBOARD.equals(value)
                || SAVE_HISTORY.equals(value)
                || RENEW_SUBSCRIPTION.equals(value)
                || SET_GOALS.equals(value)
                || SET_PROJECT_GOALS.equals(value)
                || AUTO_PAUSE.equals(value)
                || ACTIVITY_REPORT.equals(value);
          }

          @Override
          public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
            if (WEB_DASHBOARD.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_WEB_DASHBOARD_CLICK);
              BrowserUtil.browse(HUB_UI_HOST);
            } else if (SAVE_HISTORY.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_SAVE_HISTORY_CLICK);
              EnterApiKeyAction.showAction();
            } else if (RENEW_SUBSCRIPTION.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_RENEW_SUBSCRIPTION_CLICK);
              BrowserUtil.browse(HUB_UI_HOST + "/payment");
            } else if (SET_GOALS.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_SET_GOALS_CLICK);
              GoalSettingsDialog.showDialog();
            } else if (SET_PROJECT_GOALS.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_SET_PROJECT_GOALS_CLICK);
              ProjectGoalSettingsDialog.showDialog(project);
            } else if (AUTO_PAUSE.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_AUTO_PAUSE_CLICK);
              TrackingSettingsDialog.showDialog();
            } else if (ACTIVITY_REPORT.equals(selectedValue)) {
              Analytics.track(AnalyticsEventType.POPUP_ACTIVITY_REPORT_CLICK);
              ToolWindow toolWindow =
                  ToolWindowManager.getInstance(project)
                      .getToolWindow("CodeClocker Activity Report");
              if (toolWindow != null) {
                toolWindow.show();
              }
            }
            return FINAL_CHOICE;
          }

          @Override
          public boolean hasSubstep(String selectedValue) {
            return false;
          }

          @Override
          public @Nullable ListSeparator getSeparatorAbove(String value) {
            if (WEB_DASHBOARD.equals(value)
                || SAVE_HISTORY.equals(value)
                || RENEW_SUBSCRIPTION.equals(value)) {
              return new ListSeparator();
            }

            if (SET_GOALS.equals(value)) {
              return new ListSeparator();
            }

            // Goals section (first item is Daily goal)
            if (value.startsWith("Daily:") && value.contains("%")) {
              return new ListSeparator("Goals");
            }

            // Project Goals section (first item starts with project daily marker)
            if (value.startsWith("P-Daily:") && value.contains("%")) {
              return new ListSeparator("Project Goals");
            }

            // Coding Time section (after goals)
            if (value.contains("Total: ") && !value.contains("/")) {
              return new ListSeparator("Coding Time Today");
            }

            // Committed Lines section
            if (value.contains("Total: ") && value.contains("/")) {
              return new ListSeparator("Committed Lines Today");
            }

            // Trends section
            if (value.contains("Today vs. Yesterday:")) {
              return new ListSeparator("Coding Time Trends");
            }

            return null;
          }
        };

    return JBPopupFactory.getInstance().createListPopup(step);
  }

  public static String getFormattedVcsChanges() {
    return String.format("+%d / -%d", GLOBAL_ADDITIONS.get(), GLOBAL_REMOVALS.get());
  }

  private static String formatProjectVcsChanges(ProjectChangesCounters changes) {
    return String.format("+%d / -%d", changes.additions().get(), changes.removals().get());
  }

  private static String formatTodayVsYesterday(LocalActivityDataProvider dataProvider) {
    if (dataProvider == null) {
      return "Today vs. Yesterday: --";
    }
    long todaySeconds = dataProvider.getTodayTotalSeconds();
    long yesterdaySeconds = dataProvider.getYesterdayTotalSeconds();
    long diff = todaySeconds - yesterdaySeconds;
    int percentage = calculatePercentageChange(todaySeconds, yesterdaySeconds);

    return String.format(
        "Today vs. Yesterday: %s / %s", formatTimeDifference(diff), formatPercentage(percentage));
  }

  private static String formatThisWeekVsLastWeek(LocalActivityDataProvider dataProvider) {
    if (dataProvider == null) {
      return "This week vs. Last week: --";
    }
    long thisWeekSeconds = dataProvider.getWeekTotalSeconds();
    long lastWeekSeconds = dataProvider.getLastWeekTotalSeconds();
    long diff = thisWeekSeconds - lastWeekSeconds;
    int percentage = calculatePercentageChange(thisWeekSeconds, lastWeekSeconds);

    return String.format(
        "This week vs. Last week: %s / %s",
        formatTimeDifference(diff), formatPercentage(percentage));
  }

  private static int calculatePercentageChange(long current, long previous) {
    if (previous == 0) {
      return current > 0 ? 100 : 0;
    }
    return (int) Math.round(((double) (current - previous) / previous) * 100);
  }

  private static String formatTimeDifference(long diffSeconds) {
    String sign = diffSeconds >= 0 ? "+" : "-";
    long absDiff = Math.abs(diffSeconds);
    long hours = absDiff / 3600;
    long minutes = (absDiff % 3600) / 60;

    if (hours > 0) {
      return String.format("%s%dh %dm", sign, hours, minutes);
    }
    return String.format("%s%dm", sign, minutes);
  }

  private static String formatPercentage(int percentage) {
    if (percentage >= 0) {
      return String.format("↗%d%%", percentage);
    }
    return String.format("↘%d%%", Math.abs(percentage));
  }

  private static String formatGoalProgress(String label, GoalProgress progress) {
    // Add extra padding for "Daily" to match visual width of "Weekly" in proportional font
    String paddedLabel = label.equals("Daily") ? "Daily:     " : "Weekly: ";
    return String.format(
        "%s%s %s (%s)",
        paddedLabel,
        progress.renderProgressBar(15),
        progress.formatPercentage(),
        progress.formatProgress());
  }

  private static String formatProjectGoalProgress(
      String label, String projectName, GoalProgress progress) {
    // Use "P-" prefix to distinguish project goals in separator logic
    String paddedLabel = label.equals("Daily") ? "P-Daily:   " : "P-Weekly: ";
    return String.format(
        "%s%s %s (%s)",
        paddedLabel,
        progress.renderProgressBar(15),
        progress.formatPercentage(),
        progress.formatProgress());
  }
}
