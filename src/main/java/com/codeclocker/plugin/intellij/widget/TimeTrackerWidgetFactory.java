package com.codeclocker.plugin.intellij.widget;

import com.codeclocker.plugin.intellij.services.TimeTrackerWidgetService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class TimeTrackerWidgetFactory implements StatusBarWidgetFactory {

  private static final String ID = "com.codeclocker.TimeTrackerWidget";

  @Override
  public @NonNls @NotNull String getId() {
    return ID;
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "CodeClocker Time Tracker";
  }

  @Override
  public boolean isAvailable(@NotNull Project project) {
    return true;
  }

  @Override
  public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
    return project.getService(TimeTrackerWidgetService.class).getWidget();
  }

  @Override
  public void disposeWidget(@NotNull StatusBarWidget widget) {
    // Disposal is handled by the project service
  }

  @Override
  public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
    return true;
  }
}
