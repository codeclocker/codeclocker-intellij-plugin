package com.codeclocker.plugin.intellij.goal;

import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_INIT_SECONDS;
import static com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger.GLOBAL_STOP_WATCH;

import com.codeclocker.plugin.intellij.local.LocalStateRepository;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Application-level service for calculating goal progress. */
@Service(Service.Level.APP)
public final class GoalService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Get the current daily goal progress. Always returns progress using configured or default goal.
   */
  public GoalProgress getDailyProgress() {
    int goalMinutes = GoalPersistence.getDailyGoalMinutes();
    long goalSeconds = goalMinutes * 60L;
    long currentSeconds = getTotalSecondsToday();

    return GoalProgress.of(currentSeconds, goalSeconds);
  }

  /**
   * Get the current weekly goal progress. Always returns progress using configured or default goal.
   */
  public GoalProgress getWeeklyProgress() {
    int goalMinutes = GoalPersistence.getWeeklyGoalMinutes();
    long goalSeconds = goalMinutes * 60L;
    long currentSeconds = getTotalSecondsThisWeek();

    return GoalProgress.of(currentSeconds, goalSeconds);
  }

  /** Get total coded seconds for today from live tracking. */
  private long getTotalSecondsToday() {
    return GLOBAL_INIT_SECONDS.get() + GLOBAL_STOP_WATCH.getSeconds();
  }

  /**
   * Get total coded seconds for the current week (Monday to today). Combines historical data from
   * LocalStateRepository with today's live tracking.
   */
  private long getTotalSecondsThisWeek() {
    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);

    long historicalSeconds = sumSecondsFromLocalStorage(weekStart, today.minusDays(1));
    long todaySeconds = getTotalSecondsToday();

    return historicalSeconds + todaySeconds;
  }

  /** Sum coded seconds from local storage for a date range (inclusive). */
  private long sumSecondsFromLocalStorage(LocalDate startDate, LocalDate endDate) {
    LocalStateRepository repository =
        ApplicationManager.getApplication().getService(LocalStateRepository.class);

    if (repository == null) {
      return 0;
    }

    Map<String, Map<String, ProjectActivitySnapshot>> allData = repository.getAllData();
    long total = 0;

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      String datePrefix = date.format(DATE_FORMATTER);

      for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : allData.entrySet()) {
        String hourKey = hourEntry.getKey();
        if (hourKey.startsWith(datePrefix)) {
          for (ProjectActivitySnapshot snapshot : hourEntry.getValue().values()) {
            total += snapshot.getCodedTimeSeconds();
          }
        }
      }
    }

    return total;
  }
}
