package com.codeclocker.plugin.intellij.analytics;

/** Constants for analytics event types. Using constants instead of enum for extensibility. */
public final class AnalyticsEventType {

  private AnalyticsEventType() {}

  // Widget events
  public static final String STATUS_BAR_WIDGET_CLICK = "status_bar_widget_click";
  public static final String WIDGET_POPUP_OPEN = "widget_popup_open";
  public static final String WIDGET_POPUP_ACTION = "widget_popup_action";

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
}
