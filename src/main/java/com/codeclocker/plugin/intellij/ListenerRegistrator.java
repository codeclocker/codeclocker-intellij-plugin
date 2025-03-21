package com.codeclocker.plugin.intellij;

import static java.awt.AWTEvent.FOCUS_EVENT_MASK;

import com.codeclocker.plugin.intellij.apikey.ApiKeyActivationCheckerTask;
import com.codeclocker.plugin.intellij.apikey.ApiKeyPromptStartupActivity;
import com.codeclocker.plugin.intellij.listeners.CodeTrackingListener;
import com.codeclocker.plugin.intellij.listeners.FocusListener;
import com.codeclocker.plugin.intellij.reporting.DataReportingTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ListenerRegistrator implements ProjectActivity {

  private static final AtomicReference<Boolean> run = new AtomicReference<>(false);

  @Nullable
  @Override
  public Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    run.updateAndGet(
        alreadyRun -> {
          if (alreadyRun) {
            return true;
          }

          registerFocusListener();
          startDataReportingTask();
          startCheckingApiKeyStatus();
          registerChangesListener();
          ApiKeyPromptStartupActivity.showApiKeyDialog();

          return true;
        });

    return null;
  }

  private static void registerChangesListener() {
    EditorFactory.getInstance()
        .getEventMulticaster()
        .addDocumentListener(new CodeTrackingListener(), ApplicationManager.getApplication());
  }

  private static void registerFocusListener() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new FocusListener(), FOCUS_EVENT_MASK);
  }

  private static void startDataReportingTask() {
    DataReportingTask task = new DataReportingTask();
    task.schedule();
  }

  private static void startCheckingApiKeyStatus() {
    ApplicationManager.getApplication().getService(ApiKeyActivationCheckerTask.class).schedule();
  }
}
