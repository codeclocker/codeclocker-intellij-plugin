package com.codeclocker.plugin.intellij.pomodoro;

import static com.intellij.notification.NotificationType.INFORMATION;

import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import javax.swing.JFrame;

public class PomodoroNotificationService {

  public static void notifyBreakTime(boolean isLongBreak, int completedCycles) {
    if (!PomodoroPersistence.isNotificationsEnabled()) {
      return;
    }

    int workMinutes = PomodoroPersistence.getWorkMinutes();
    String breakType = isLongBreak ? "long" : "short";
    int breakMinutes =
        isLongBreak
            ? PomodoroPersistence.getLongBreakMinutes()
            : PomodoroPersistence.getShortBreakMinutes();

    String title = "🍅 CodeClocker: Time for a break!";
    String content =
        String.format(
            "You've been coding for %dm. Take a %s %dm break.%s",
            workMinutes,
            breakType,
            breakMinutes,
            isLongBreak ? String.format(" You've completed %d cycles!", completedCycles) : "");

    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(title, content, INFORMATION)
                    .addAction(
                        NotificationAction.createSimpleExpiring(
                            "Start break",
                            () -> {
                              PomodoroTimerService svc =
                                  ApplicationManager.getApplication()
                                      .getService(PomodoroTimerService.class);
                              if (svc != null) {
                                svc.startBreak(isLongBreak);
                              }
                            }))
                    .addAction(
                        NotificationAction.createSimpleExpiring(
                            "Skip break",
                            () -> {
                              PomodoroTimerService svc =
                                  ApplicationManager.getApplication()
                                      .getService(PomodoroTimerService.class);
                              if (svc != null) {
                                svc.skipBreak();
                              }
                            }))
                    .addAction(
                        NotificationAction.createSimpleExpiring(
                            "Stop Pomodoro",
                            () -> {
                              PomodoroTimerService svc =
                                  ApplicationManager.getApplication()
                                      .getService(PomodoroTimerService.class);
                              if (svc != null) {
                                svc.stop();
                              }
                            }))
                    .notify(getCurrentProject()));
  }

  public static void notifyBreakOver() {
    if (!PomodoroPersistence.isNotificationsEnabled()) {
      return;
    }

    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CodeClocker")
                    .createNotification(
                        "⏰ CodeClocker: Break is over!",
                        "Ready for another coding round? 🍅",
                        INFORMATION)
                    .addAction(
                        NotificationAction.createSimpleExpiring(
                            "Start working",
                            () -> {
                              PomodoroTimerService svc =
                                  ApplicationManager.getApplication()
                                      .getService(PomodoroTimerService.class);
                              if (svc != null) {
                                svc.start();
                              }
                            }))
                    .addAction(
                        NotificationAction.createSimpleExpiring(
                            "Stop Pomodoro",
                            () -> {
                              PomodoroTimerService svc =
                                  ApplicationManager.getApplication()
                                      .getService(PomodoroTimerService.class);
                              if (svc != null) {
                                svc.stop();
                              }
                            }))
                    .notify(getCurrentProject()));
  }

  private static Project getCurrentProject() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      JFrame frame = WindowManager.getInstance().getFrame(project);
      if (frame != null && frame.isActive()) {
        return project;
      }
    }
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    return projects.length > 0 ? projects[0] : null;
  }
}
