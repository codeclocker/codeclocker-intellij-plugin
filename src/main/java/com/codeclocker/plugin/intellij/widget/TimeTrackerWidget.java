package com.codeclocker.plugin.intellij.widget;

import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import com.intellij.util.IconUtil;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerWidget
    implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {

  private static final Logger LOG = Logger.getInstance(TimeTrackerWidget.class);
  private static final String WIDGET_ID = "com.codeclocker.TimeTrackerWidget";
  private static final Icon WIDGET_ICON = loadScaledIcon();

  private final Project project;
  private final TimeTrackerWidgetService service;
  private StatusBar statusBar;

  private static Icon loadScaledIcon() {
    Icon icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", TimeTrackerWidget.class);
    // Scale to 16x16 for status bar
    return IconUtil.scale(icon, null, 16.0f / icon.getIconWidth());
  }

  public TimeTrackerWidget(Project project, TimeTrackerWidgetService service) {
    LOG.info("TimeTrackerWidget constructor called for project: " + project.getName());
    this.project = project;
    this.service = service;
  }

  @Override
  public @NotNull String ID() {
    return WIDGET_ID;
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    LOG.info("TimeTrackerWidget install() called");
    this.statusBar = statusBar;
  }

  @Override
  public void dispose() {
    LOG.info("TimeTrackerWidget dispose() called");
  }

  @Override
  public @Nullable WidgetPresentation getPresentation() {
    return this;
  }

  // MultipleTextValuesPresentation methods
  @Nullable
  @Override
  public String getSelectedValue() {
    if (project == null || service == null) {
      return "";
    }
    String totalTime = service.getFormattedTotalTime();
    String projectTime = service.getFormattedProjectTime();
    return "Total: " + totalTime + " | Project: " + projectTime;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return WIDGET_ICON;
  }

  @Nullable
  @Override
  public String getTooltipText() {
    if (project == null || service == null) {
      return null;
    }
    String totalTime = service.getFormattedTotalTime();
    String projectTime = service.getFormattedProjectTime();
    return "Total coding time today: "
        + totalTime
        + ". Time on "
        + project.getName()
        + ": "
        + projectTime;
  }

  @Nullable
  @Override
  public Consumer<MouseEvent> getClickConsumer() {
    return null;
  }

  public void updateText() {
    // Notify the status bar to update this widget
    if (statusBar != null) {
      statusBar.updateWidget(WIDGET_ID);
    } else {
      // Fallback: try to get status bar from WindowManager
      StatusBar fallbackStatusBar = WindowManager.getInstance().getStatusBar(project);
      if (fallbackStatusBar != null) {
        fallbackStatusBar.updateWidget(WIDGET_ID);
      }
    }
  }
}
