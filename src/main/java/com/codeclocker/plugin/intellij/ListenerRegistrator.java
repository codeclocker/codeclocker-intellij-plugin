package com.codeclocker.plugin.intellij;

import static com.codeclocker.plugin.intellij.widget.TimeTrackerInitializer.initializeTimerWidgets;
import static java.awt.AWTEvent.FOCUS_EVENT_MASK;

import com.codeclocker.plugin.intellij.analytics.AnalyticsReportingTask;
import com.codeclocker.plugin.intellij.apikey.ApiKeyPromptStartupActivity;
import com.codeclocker.plugin.intellij.listeners.FocusListener;
import com.codeclocker.plugin.intellij.reporting.DataReportingTask;
import com.codeclocker.plugin.intellij.reporting.TimeComparisonFetchTask;
import com.codeclocker.plugin.intellij.services.BranchActivityTracker;
import com.codeclocker.plugin.intellij.subscription.SubscriptionStateCheckerTask;
import com.intellij.openapi.application.ApplicationManager;
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
          startTimeComparisonFetchTask();
          startAnalyticsReportingTask();
          ApiKeyPromptStartupActivity.showApiKeyDialog();
          initializeTimerWidgets();

          return true;
        });

    // Initialize branch tracking for this project
    initializeBranchTracking(project);

    return null;
  }

  private static void initializeBranchTracking(Project project) {
    BranchActivityTracker branchTracker =
        ApplicationManager.getApplication().getService(BranchActivityTracker.class);
    if (branchTracker != null) {
      branchTracker.initializeFromGit(project);
    }
  }

  private static void registerFocusListener() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new FocusListener(), FOCUS_EVENT_MASK);
  }

  private static void startDataReportingTask() {
    ApplicationManager.getApplication().getService(DataReportingTask.class).schedule();
  }

  private static void startCheckingApiKeyStatus() {
    ApplicationManager.getApplication().getService(SubscriptionStateCheckerTask.class).schedule();
  }

  private static void startTimeComparisonFetchTask() {
    ApplicationManager.getApplication().getService(TimeComparisonFetchTask.class).schedule();
  }

  private static void startAnalyticsReportingTask() {
    ApplicationManager.getApplication().getService(AnalyticsReportingTask.class).schedule();
  }
}
