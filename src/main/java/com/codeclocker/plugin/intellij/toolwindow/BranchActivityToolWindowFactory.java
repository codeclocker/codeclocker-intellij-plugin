package com.codeclocker.plugin.intellij.toolwindow;

import com.codeclocker.plugin.intellij.dashboard.DashboardPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/** Factory for creating the Branch Activity tool window with Dashboard and Activity tabs. */
public class BranchActivityToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ContentFactory contentFactory = ContentFactory.getInstance();

    // Dashboard tab (first position)
    DashboardPanel dashboardPanel = new DashboardPanel();
    Content dashboardContent = contentFactory.createContent(dashboardPanel, "Dashboard", false);
    toolWindow.getContentManager().addContent(dashboardContent);

    // Activity tab (existing)
    BranchActivityPanel panel = new BranchActivityPanel(project);
    Content activityContent = contentFactory.createContent(panel, "Activity", false);
    toolWindow.getContentManager().addContent(activityContent);
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return true;
  }
}
