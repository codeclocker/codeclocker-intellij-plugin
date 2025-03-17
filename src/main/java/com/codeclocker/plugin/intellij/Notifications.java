package com.codeclocker.plugin.intellij;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static com.intellij.notification.NotificationType.WARNING;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

public class Notifications {

  private static final Logger LOG = Logger.getInstance(Notifications.class);

  public static final String SUBSCRIPTION_WILL_EXPIRE_SOON_NOTIFICATION_SHOWN =
      "com.codeclocker.subscription-will-expire-soon-notification-shown";
  public static final String SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN =
      "com.codeclocker.subscription-expired-notification-shown";

  public static void showInvalidApiKeyNotification() {
    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "Invalid CodeClocker API key", "Your CodeClocker API key is invalid.", WARNING)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    "Renew now", () -> BrowserUtil.browse(HUB_UI_HOST + "/api-key")));

    ApplicationManager.getApplication().invokeLater(() -> notification.notify(getCurrentProject()));
  }

  public static void showSubscriptionWillExpireSoonNotification() { // todo: decide how to reset
    if (checkAndSet(SUBSCRIPTION_WILL_EXPIRE_SOON_NOTIFICATION_SHOWN)) {
      return;
    }

    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "CodeClocker subscription will expire soon",
                "Your CodeClocker subscription will expire soon. Renew now to continue tracking activity without interruptions.",
                WARNING)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    "Renew now", () -> BrowserUtil.browse(HUB_UI_HOST + "/api-key")));

    ApplicationManager.getApplication().invokeLater(() -> notification.notify(getCurrentProject()));
  }

  public static void showPaymentExpiredNotification() {
    if (checkAndSet(SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN)) {
      return;
    }

    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "CodeClocker subscription expired",
                "Your CodeClocker subscription expired. Renew now to keep tracking activity.",
                WARNING)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    "Renew now", () -> BrowserUtil.browse(HUB_UI_HOST + "/api-key")));

    ApplicationManager.getApplication().invokeLater(() -> notification.notify(getCurrentProject()));
  }

  private static Project getCurrentProject() {
    DataContext dataContext = DataManager.getInstance().getDataContext(null);
    return dataContext.getData(CommonDataKeys.PROJECT);
  }

  private static boolean checkAndSet(String property) {
    PropertiesComponent properties = PropertiesComponent.getInstance();
    boolean shown = properties.getBoolean(property, false);
    LOG.debug("Shown [{}] by property [{}]", shown, property);
    if (!shown) {
      properties.setValue(property, true);
    }

    return shown;
  }
}
