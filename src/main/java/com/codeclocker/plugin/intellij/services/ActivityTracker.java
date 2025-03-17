package com.codeclocker.plugin.intellij.services;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class ActivityTracker implements Disposable {

  private static final Logger LOG = Logger.getInstance(ActivityTracker.class);

  private final Map<String, Map<String, TimeSpentPerFileLogger>> fileByModuleByProject =
      new ConcurrentHashMap<>();
  private final long pauseActivityAfterInactivityMillis = Duration.ofSeconds(30).toMillis();
  private final AtomicReference<ScheduledFuture<?>> scheduledTask;
  private long lastRescheduledAt;
  private final AtomicReference<Pair<String, String>> currentProjectToModule =
      new AtomicReference<>();

  public ActivityTracker() {
    this.scheduledTask = new AtomicReference<>(schedule());
  }

  public Map<String, Map<String, TimeSpentPerFileLogger>> getFileByModuleByProject() {
    return fileByModuleByProject;
  }

  public Map<String, Map<String, TimeSpentPerFileLogger>> drainActivitySample() {
    Map<String, Map<String, TimeSpentPerFileLogger>> drainTo = new HashMap<>(fileByModuleByProject);
    fileByModuleByProject.clear(); // todo: atomicity

    return drainTo;
  }

  public void logAdditions(Project project, Module module, VirtualFile file, long additions) {
    logChanges(project, module, file, sample -> sample.incrementAdditions(additions));
  }

  public void logRemovals(Project project, Module module, VirtualFile file, long removals) {
    logChanges(project, module, file, sample -> sample.incrementRemovals(removals));
  }

  private void logChanges(
      Project project, Module module, VirtualFile file, Consumer<TimeSpentPerFileSample> consumer) {
    fileByModuleByProject.compute(
        project.getName(),
        (name, byModule) -> logChanges(module, byModule, logger -> logger.compute(file, consumer)));
  }

  private static Map<String, TimeSpentPerFileLogger> logChanges(
      Module module,
      @Nullable Map<String, TimeSpentPerFileLogger> byModule,
      Consumer<TimeSpentPerFileLogger> consumer) {
    if (byModule == null) {
      byModule = new ConcurrentHashMap<>();
    }

    byModule.compute(module.getName(), (moduleName, byFile) -> logChanges(byFile, consumer));
    return byModule;
  }

  private static TimeSpentPerFileLogger logChanges(
      @Nullable TimeSpentPerFileLogger byFile, Consumer<TimeSpentPerFileLogger> consumer) {
    if (byFile == null) {
      byFile = new TimeSpentPerFileLogger();
    }
    consumer.accept(byFile);
    return byFile;
  }

  public void logTime(Project project, Module module, VirtualFile file) {
    pauseCurrentFile(project, module);
    rescheduleInactivityTask();
    fileByModuleByProject.compute(
        project.getName(),
        (projectName, byModule) -> logChanges(module, byModule, logger -> logger.log(file)));
  }

  public void rescheduleInactivityTask() {
    long now = System.currentTimeMillis();
    if (now - lastRescheduledAt < 1000) {
      return;
    }
    lastRescheduledAt = now;

    scheduledTask.updateAndGet(
        currentTask -> {
          currentTask.cancel(false);
          return schedule();
        });
  }

  private void pauseCurrentFile(Project project, Module module) {
    currentProjectToModule.updateAndGet(
        current -> {
          if (current == null
              || (Objects.equals(current.getLeft(), project.getName())
                  && Objects.equals(current.getRight(), module.getName()))) {
            LOG.debug("Active project and module didn't change");

            return Pair.of(project.getName(), module.getName());
          }

          pauseCurrent(current);

          return Pair.of(project.getName(), module.getName());
        });
  }

  private ScheduledFuture<?> schedule() {
    return EXECUTOR.schedule(this::pause, pauseActivityAfterInactivityMillis, MILLISECONDS);
  }

  public void pause() {
    currentProjectToModule.updateAndGet(
        current -> {
          if (current == null) {
            return null;
          }

          pauseCurrent(current);

          return current;
        });
  }

  private void pauseCurrent(Pair<String, String> current) {
    fileByModuleByProject.compute(
        current.getLeft(),
        (project, byModule) -> {
          if (byModule == null) {
            return new ConcurrentHashMap<>();
          }
          byModule.compute(
              current.getRight(),
              (moduleName, byFile) -> {
                if (byFile == null) {
                  return new TimeSpentPerFileLogger();
                }
                byFile.pauseDueToInactivity();

                return byFile;
              });

          return byModule;
        });
  }

  @Override
  public void dispose() {
    scheduledTask.get().cancel(false);
    EXECUTOR.shutdown();
  }
}
