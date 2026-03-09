package com.codeclocker.plugin.intellij.widget;

import com.codeclocker.plugin.intellij.goal.GoalPersistence;
import com.codeclocker.plugin.intellij.goal.GoalService;
import com.codeclocker.plugin.intellij.pomodoro.PomodoroState;
import com.codeclocker.plugin.intellij.pomodoro.PomodoroTimerService;
import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.IconUtil;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerWidget
    implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

  private static final Logger LOG = Logger.getInstance(TimeTrackerWidget.class);
  private static final String WIDGET_ID = "com.codeclocker.TimeTrackerWidget";

  // todo: Prefer a small, monochrome SVG specifically for the status bar
  // e.g., "/icons/status_time.svg" sized for 16px. For SVGs, the platform scales automatically.
  private static final Icon WIDGET_ICON = loadScaledIcon();

  private final Project project;
  private final TimeTrackerWidgetService service;

  private StatusBar statusBar;
  private JBPopup popup;

  private static Icon loadScaledIcon() {
    Icon icon = IconLoader.getIcon("/META-INF/statusBarWidgetIcon.svg", TimeTrackerWidget.class);
    return IconUtil.scale(icon, null, 16.0f / icon.getIconWidth());
  }

  public TimeTrackerWidget(Project project, TimeTrackerWidgetService service) {
    LOG.debug("TimeTrackerWidget constructor for project: " + project.getName());
    this.project = project;
    this.service = service;
  }

  @Override
  public @NotNull String ID() {
    return WIDGET_ID;
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    this.statusBar = statusBar;
  }

  @Override
  public void dispose() {
    if (popup != null && !popup.isDisposed()) {
      popup.cancel();
      popup = null;
    }
  }

  @Override
  public @Nullable WidgetPresentation getPresentation() {
    return this;
  }

  @Override
  public String getSelectedValue() {
    String totalTime = service.getFormattedTotalTime();
    String projectTime = service.getFormattedProjectTime();

    String base;
    if (GoalPersistence.isGoalsEnabled()) {
      GoalService goalService = ApplicationManager.getApplication().getService(GoalService.class);
      String goalPercentage =
          goalService != null ? goalService.getDailyProgress().formatPercentage() : "0%";
      base = "Total: " + totalTime + " (" + goalPercentage + ") | Project: " + projectTime;
    } else {
      base = "Total: " + totalTime + " | Project: " + projectTime;
    }

    String pomodoroSuffix = getPomodoroSuffix();
    return pomodoroSuffix.isEmpty() ? base : base + pomodoroSuffix;
  }

  private String getPomodoroSuffix() {
    PomodoroTimerService svc =
        ApplicationManager.getApplication().getService(PomodoroTimerService.class);
    if (svc == null) {
      return "";
    }

    PomodoroState pomodoroState = svc.getState();
    if (pomodoroState == PomodoroState.WORKING) {
      return " | Pomodoro: " + svc.getFormattedWorkRemaining() + " left";
    } else if (pomodoroState == PomodoroState.BREAK) {
      return " | Break: " + svc.getFormattedBreakRemaining() + " left";
    }
    return "";
  }

  @Override
  public Icon getIcon() {
    return WIDGET_ICON;
  }

  @Override
  public String getTooltipText() {
    String totalTime = service.getFormattedTotalTime();
    String projectTime = service.getFormattedProjectTime();

    String tooltip =
        "Total today: " + totalTime + ". Time on " + project.getName() + ": " + projectTime + ".";

    PomodoroTimerService svc =
        ApplicationManager.getApplication().getService(PomodoroTimerService.class);
    if (svc != null) {
      PomodoroState pomodoroState = svc.getState();
      if (pomodoroState == PomodoroState.WORKING) {
        tooltip +=
            " Pomodoro: "
                + svc.getFormattedWorkRemaining()
                + " left (cycle "
                + (svc.getCompletedCycles() + 1)
                + "/"
                + com.codeclocker.plugin.intellij.pomodoro.PomodoroPersistence
                    .getCyclesBeforeLongBreak()
                + ").";
      } else if (pomodoroState == PomodoroState.BREAK) {
        tooltip += " Break: " + svc.getFormattedBreakRemaining() + " left.";
      }
    }

    tooltip += " Click to see more info.";
    return tooltip;
  }

  @Nullable
  @Override
  public ListPopup getPopup() {
    String totalTime = service.getFormattedTotalTime();
    String projectTime = service.getFormattedProjectTime();

    return TimeTrackerPopup.create(project, totalTime, projectTime);
  }

  public void updateText() {
    // Always get the current status bar from WindowManager to ensure we update the correct one
    // The statusBar field might be stale if the project window was reopened
    StatusBar currentStatusBar = WindowManager.getInstance().getStatusBar(project);
    if (currentStatusBar != null) {
      currentStatusBar.updateWidget(WIDGET_ID);
    } else if (statusBar != null) {
      // Fallback to the installed status bar reference
      statusBar.updateWidget(WIDGET_ID);
    }
  }
}
