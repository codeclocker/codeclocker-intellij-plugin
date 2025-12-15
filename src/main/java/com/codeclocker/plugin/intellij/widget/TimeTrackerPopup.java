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
import com.codeclocker.plugin.intellij.reporting.TimeComparisonFetchTask;
import com.codeclocker.plugin.intellij.reporting.TimeComparisonHttpClient.TimePeriodComparisonDto;
import com.codeclocker.plugin.intellij.services.vcs.ChangesActivityTracker;
import com.codeclocker.plugin.intellij.services.vcs.ProjectChangesCounters;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerPopup {

  private static final String WEB_DASHBOARD = "Web Dashboard →";
  private static final String SAVE_HISTORY = "Save my history & unlock trends →";
  private static final String RENEW_SUBSCRIPTION = "Renew subscription for trends →";

  public static ListPopup create(Project project, String totalTime, String projectTime) {
    ChangesActivityTracker tracker =
        ApplicationManager.getApplication().getService(ChangesActivityTracker.class);
    ProjectChangesCounters projectChanges = tracker.getProjectChanges(project.getName());

    TimeComparisonFetchTask comparisonTask = TimeComparisonFetchTask.getInstance();

    List<String> items = new ArrayList<>();
    items.add("Total: " + totalTime);
    items.add(project.getName() + ": " + projectTime);
    items.add("Total: " + getFormattedVcsChanges());
    items.add(project.getName() + ": " + formatProjectVcsChanges(projectChanges));
    items.add(formatTodayVsYesterday(comparisonTask.getTodayVsYesterday()));
    items.add(formatThisWeekVsLastWeek(comparisonTask.getThisWeekVsLastWeek()));

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
                || RENEW_SUBSCRIPTION.equals(value);
          }

          @Override
          public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
            if (WEB_DASHBOARD.equals(selectedValue)) {
              Analytics.track(
                  AnalyticsEventType.WIDGET_POPUP_ACTION, Map.of("action", "web_dashboard"));
              BrowserUtil.browse(HUB_UI_HOST);
            } else if (SAVE_HISTORY.equals(selectedValue)) {
              Analytics.track(
                  AnalyticsEventType.WIDGET_POPUP_ACTION, Map.of("action", "save_history"));
              EnterApiKeyAction.showAction();
            } else if (RENEW_SUBSCRIPTION.equals(selectedValue)) {
              Analytics.track(
                  AnalyticsEventType.WIDGET_POPUP_ACTION, Map.of("action", "renew_subscription"));
              BrowserUtil.browse(HUB_UI_HOST + "/payment");
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

            if (value.contains("Total: ") && value.contains("/")) {
              return new ListSeparator("Committed Lines Today");
            }

            if (value.contains("Total: ") && !value.contains("/")) {
              return new ListSeparator("Coding Time Today");
            }

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

  private static String formatTodayVsYesterday(TimePeriodComparisonDto comparison) {
    if (comparison == null) {
      return "Today vs. Yesterday: --";
    }
    return String.format(
        "Today vs. Yesterday: %s / %s",
        formatTimeDifference(comparison.differenceSeconds()),
        formatPercentage(comparison.percentageChange()));
  }

  private static String formatThisWeekVsLastWeek(TimePeriodComparisonDto comparison) {
    if (comparison == null) {
      return "This week vs. Last week: --";
    }
    return String.format(
        "This week vs. Last week: %s / %s",
        formatTimeDifference(comparison.differenceSeconds()),
        formatPercentage(comparison.percentageChange()));
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
}
