package com.codeclocker.plugin.intellij.analytics;

public final class AnalyticsEventType {

  private AnalyticsEventType() {}

  // Widget events
  public static final String STATUS_BAR_WIDGET_CLICK = "status_bar_widget_click";

  // Widget popup actions
  public static final String POPUP_WEB_DASHBOARD_CLICK = "popup_web_dashboard_click";
  public static final String POPUP_SAVE_HISTORY_CLICK = "popup_save_history_click";
  public static final String POPUP_RENEW_SUBSCRIPTION_CLICK = "popup_renew_subscription_click";
  public static final String POPUP_SET_GOALS_CLICK = "popup_set_goals_click";
  public static final String POPUP_SET_PROJECT_GOALS_CLICK = "popup_set_project_goals_click";
  public static final String POPUP_AUTO_PAUSE_CLICK = "popup_auto_pause_click";
  public static final String POPUP_DASHBOARD_CLICK = "popup_dashboard_click";
  public static final String POPUP_ACTIVITY_REPORT_CLICK = "popup_activity_report_click";

  // Plugin lifecycle events
  public static final String PLUGIN_STARTED = "plugin_started";
  public static final String PLUGIN_STOPPED = "plugin_stopped";

  // Onboarding tour events
  public static final String TOUR_WELCOME_START = "tour_welcome_start";
  public static final String TOUR_WELCOME_SKIP = "tour_welcome_skip";
  public static final String TOUR_STATUS_BAR_NEXT = "tour_status_bar_next";
  public static final String TOUR_STATUS_BAR_SKIP = "tour_status_bar_skip";
  public static final String TOUR_ACTIVITY_POPUP_NEXT = "tour_activity_popup_next";
  public static final String TOUR_ACTIVITY_POPUP_SKIP = "tour_activity_popup_skip";
  public static final String TOUR_GOALS_SET = "tour_goals_set";
  public static final String TOUR_GOALS_LATER = "tour_goals_later";
  public static final String TOUR_GOALS_SKIP = "tour_goals_skip";
  public static final String TOUR_HUB_CONNECT = "tour_hub_connect";
  public static final String TOUR_HUB_SKIP = "tour_hub_skip";

  // Goal events
  public static final String SET_NEW_GOAL = "set_new_goal";
}
