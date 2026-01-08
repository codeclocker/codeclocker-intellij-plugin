package com.codeclocker.plugin.intellij.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/** Factory for creating the Branch Activity tool window. */
public class BranchActivityToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    BranchActivityPanel panel = new BranchActivityPanel(project);
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(panel, "Activity", false);
    toolWindow.getContentManager().addContent(content);
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return true;
  }
}
