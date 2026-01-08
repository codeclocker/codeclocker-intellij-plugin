package com.codeclocker.plugin.intellij.analytics;

/** Constants for analytics event types. Using constants instead of enum for extensibility. */
public final class AnalyticsEventType {

  private AnalyticsEventType() {}

  // Widget events
  public static final String STATUS_BAR_WIDGET_CLICK = "status_bar_widget_click";
  public static final String WIDGET_POPUP_OPEN = "widget_popup_open";

  // Widget popup actions
  public static final String POPUP_WEB_DASHBOARD_CLICK = "popup_web_dashboard_click";
  public static final String POPUP_SAVE_HISTORY_CLICK = "popup_save_history_click";
  public static final String POPUP_RENEW_SUBSCRIPTION_CLICK = "popup_renew_subscription_click";
  public static final String POPUP_SET_GOALS_CLICK = "popup_set_goals_click";
  public static final String POPUP_AUTO_PAUSE_CLICK = "popup_auto_pause_click";
  public static final String POPUP_ACTIVITY_REPORT_CLICK = "popup_activity_report_click";

  // API key events
  public static final String API_KEY_ENTERED = "api_key_entered";
  public static final String API_KEY_CLEARED = "api_key_cleared";
  public static final String API_KEY_DIALOG_OPENED = "api_key_dialog_opened";
  public static final String API_KEY_DIALOG_CANCELLED = "api_key_dialog_cancelled";

  // Plugin lifecycle events
  public static final String PLUGIN_STARTED = "plugin_started";
  public static final String PLUGIN_STOPPED = "plugin_stopped";

  // Action events
  public static final String ACTION_SHOW_STATISTICS = "action_show_statistics";

  // Feature usage events
  public static final String FEATURE_LOCAL_STORAGE_SYNC = "feature_local_storage_sync";

  // Error events
  public static final String ERROR_DATA_REPORT_FAILED = "error_data_report_failed";
  public static final String ERROR_API_REQUEST_FAILED = "error_api_request_failed";

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
