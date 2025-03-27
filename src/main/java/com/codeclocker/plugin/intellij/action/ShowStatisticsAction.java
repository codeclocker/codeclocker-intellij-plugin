package com.codeclocker.plugin.intellij.action;

import com.codeclocker.plugin.intellij.services.TimeSpentActivityTracker;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ShowStatisticsAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    TimeSpentActivityTracker activityTracker =
        ApplicationManager.getApplication().getService(TimeSpentActivityTracker.class);
    StringBuilder stats = new StringBuilder();

    //    Map<String, Map<String, TimeSpentPerFileLogger>> raw =
    //        activityTracker.getFileByModuleByProject();
    //    String json = new Gson().toJson(raw);
    //
    //    stats.append(MyBundle.message("timeSpentPerProject")).append("\n");
    //    activityTracker.getTimeSpentPerProject().forEach((name, time) -> {
    //      stats.append(name).append(": ")
    //          .append(Duration.ofNanos(time.timeSpent().get().getNanoTime()).getSeconds())
    //          .append(" seconds; ").append(time.additions().get()).append(" added lines; ")
    //          .append(time.removals().get()).append(" removed lines").append("\n");
    //    });
    //
    //    stats.append("\n").append(MyBundle.message("timeSpentPerFile")).append("\n");
    //    activityTracker.getTimeSpentPerFile(project).forEach((name, time) -> {
    //      stats.append(name).append(": ")
    //          .append(Duration.ofNanos(time.timeSpent().get().getNanoTime()).getSeconds())
    //          .append(" seconds; ").append(time.additions().get()).append(" added lines; ")
    //          .append(time.removals().get()).append(" removed lines").append("\n");
    //    });
    //
    //    stats.append("\n").append(MyBundle.message("timeSpentPerModule")).append("\n");
    //    activityTracker.getTimeSpentPerModule(project).forEach((name, time) -> {
    //      stats.append(name).append(": ")
    //          .append(Duration.ofNanos(time.timeSpent().get().getNanoTime()).getSeconds())
    //          .append(" seconds; ").append(time.additions().get()).append(" added lines; ")
    //          .append(time.removals().get()).append(" removed lines").append("\n");
    //    });

    // Show statistics in a dialog
    //    Messages.showMessageDialog(
    //        project, json, Bundle.message("statisticPopupTitle"), Messages.getInformationIcon());
  }
}
