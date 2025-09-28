package com.codeclocker.plugin.intellij;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static com.intellij.notification.NotificationType.WARNING;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationService {

  public static final AtomicBoolean SUBSCRIPTION_WILL_EXPIRE_SOON_NOTIFICATION_SHOWN =
      new AtomicBoolean();
  public static final AtomicBoolean SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN = new AtomicBoolean();

  public void showInvalidApiKeyNotification() {
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

  public void showSubscriptionWillExpireSoonNotification() {
    if (SUBSCRIPTION_WILL_EXPIRE_SOON_NOTIFICATION_SHOWN.getAndSet(true)) {
      return;
    }

    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "CodeClocker subscription will expire soon",
                "Your CodeClocker subscription will expire soon. Renew it now to keep tracking activity.",
                WARNING)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    "Renew now", () -> BrowserUtil.browse(HUB_UI_HOST + "/payment")));

    ApplicationManager.getApplication().invokeLater(() -> notification.notify(getCurrentProject()));
  }

  public void showSubscriptionExpiredNotification() {
    if (SUBSCRIPTION_EXPIRED_NOTIFICATION_SHOWN.getAndSet(true)) {
      return;
    }

    Notification notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeClocker")
            .createNotification(
                "CodeClocker subscription expired",
                "Your CodeClocker subscription expired. Renew it now to keep tracking activity.",
                WARNING)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    "Renew now", () -> BrowserUtil.browse(HUB_UI_HOST + "/payment")));

    ApplicationManager.getApplication().invokeLater(() -> notification.notify(getCurrentProject()));
  }

  private static Project getCurrentProject() {
    DataContext dataContext = DataManager.getInstance().getDataContext(null);
    return dataContext.getData(CommonDataKeys.PROJECT);
  }
}
