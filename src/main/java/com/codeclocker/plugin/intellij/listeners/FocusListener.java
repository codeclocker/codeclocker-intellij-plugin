package com.codeclocker.plugin.intellij.listeners;

import com.codeclocker.plugin.intellij.services.TimeSpentActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;

public class FocusListener implements AWTEventListener, Disposable {

  private static final Logger LOG = Logger.getInstance(FocusListener.class);
  private final TimeSpentActivityTracker tracker;
  private final DataManager dataManager;

  public FocusListener() {
    this.dataManager = DataManager.getInstance();
    this.tracker = ApplicationManager.getApplication().getService(TimeSpentActivityTracker.class);
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (!(event instanceof FocusEvent focusEvent)) {
      return;
    }

    if (focusEvent.getID() != FocusEvent.FOCUS_GAINED) {
      return;
    }

    Component component = focusEvent.getComponent();
    if (component == null) {
      return;
    }

    Project project = getProject(component);
    if (project == null) {
      LOG.error("Project is null. Doing nothing");
      return;
    }

    tracker.logTime(project.getName());
  }

  @Nullable
  private Project getProject(Component component) {
    Project project = null;

    if (component instanceof JComponent jComponent) {
      project = (Project) jComponent.getClientProperty(CommonDataKeys.PROJECT.getName());
    }

    if (project == null) {
      project = CommonDataKeys.PROJECT.getData(dataManager.getDataContext(component));
    }

    if (component instanceof JComponent jComponent) {
      jComponent.putClientProperty(CommonDataKeys.PROJECT.getName(), project);
    }

    return project;
  }

  @Override
  public void dispose() {
    Toolkit.getDefaultToolkit().removeAWTEventListener(this);
  }
}
