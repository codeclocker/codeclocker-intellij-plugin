package com.codeclocker.plugin.intellij.local;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides local activity data converted to the user's local timezone for display purposes. This is
 * the single source of truth for all UI components that need to display coding time data.
 *
 * <p>Internally, data is stored in UTC. This provider converts UTC hourKeys to local timezone when
 * returning data for display.
 */
@Service(Service.Level.APP)
public final class LocalActivityDataProvider {

  private static final DateTimeFormatter DATETIME_HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
  private static final ZoneId UTC = ZoneId.of("UTC");

  private LocalStateRepository getRepository() {
    return ApplicationManager.getApplication().getService(LocalStateRepository.class);
  }

  /**
   * Returns all activity data with hourKeys converted to local timezone. The returned map is sorted
   * by hourKey in descending order (most recent first).
   *
   * @return Map of localHourKey -> (projectName -> snapshot)
   */
  public Map<String, Map<String, ProjectActivitySnapshot>> getAllDataInLocalTimezone() {
    Map<String, Map<String, ProjectActivitySnapshot>> utcData = getRepository().getAllData();
    return convertToLocalTimezone(utcData);
  }

  /**
   * Get total coded seconds for today across all projects.
   *
   * @return total seconds coded today in local timezone
   */
  public long getTodayTotalSeconds() {
    String todayLocalPrefix = LocalDate.now().toString();
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(todayLocalPrefix))
        .flatMap(entry -> entry.getValue().values().stream())
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /**
   * Get total coded seconds for today for a specific project.
   *
   * @param projectName the project name
   * @return total seconds coded today for the project in local timezone
   */
  public long getTodayProjectSeconds(String projectName) {
    String todayLocalPrefix = LocalDate.now().toString();
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(todayLocalPrefix))
        .map(entry -> entry.getValue().get(projectName))
        .filter(snapshot -> snapshot != null)
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /**
   * Get total coded seconds for the current week (Monday to Sunday) across all projects.
   *
   * @return total seconds coded this week in local timezone
   */
  public long getWeekTotalSeconds() {
    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> isInWeek(entry.getKey(), weekStart, today))
        .flatMap(entry -> entry.getValue().values().stream())
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /**
   * Get total coded seconds for the current week (Monday to Sunday) for a specific project.
   *
   * @param projectName the project name
   * @return total seconds coded this week for the project in local timezone
   */
  public long getWeekProjectSeconds(String projectName) {
    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> isInWeek(entry.getKey(), weekStart, today))
        .map(entry -> entry.getValue().get(projectName))
        .filter(snapshot -> snapshot != null)
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /**
   * Get total coded seconds for yesterday across all projects.
   *
   * @return total seconds coded yesterday in local timezone
   */
  public long getYesterdayTotalSeconds() {
    String yesterdayPrefix = LocalDate.now().minusDays(1).toString();
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(yesterdayPrefix))
        .flatMap(entry -> entry.getValue().values().stream())
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  /**
   * Get total coded seconds for last week (Monday to Sunday) across all projects.
   *
   * @return total seconds coded last week in local timezone
   */
  public long getLastWeekTotalSeconds() {
    LocalDate today = LocalDate.now();
    LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
    LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);
    Map<String, Map<String, ProjectActivitySnapshot>> localData = getAllDataInLocalTimezone();

    return localData.entrySet().stream()
        .filter(entry -> isInDateRange(entry.getKey(), startOfLastWeek, endOfLastWeek))
        .flatMap(entry -> entry.getValue().values().stream())
        .mapToLong(ProjectActivitySnapshot::getCodedTimeSeconds)
        .sum();
  }

  private boolean isInWeek(String hourKey, LocalDate weekStart, LocalDate today) {
    return isInDateRange(hourKey, weekStart, today);
  }

  private boolean isInDateRange(String hourKey, LocalDate start, LocalDate end) {
    try {
      String dateStr = hourKey.substring(0, 10); // "yyyy-MM-dd"
      LocalDate date = LocalDate.parse(dateStr);
      return !date.isBefore(start) && !date.isAfter(end);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Converts a map with UTC hourKeys to local timezone hourKeys.
   *
   * @param utcData data with UTC hourKeys
   * @return data with local timezone hourKeys, sorted by key descending
   */
  private Map<String, Map<String, ProjectActivitySnapshot>> convertToLocalTimezone(
      Map<String, Map<String, ProjectActivitySnapshot>> utcData) {

    ZoneId localZone = ZoneId.systemDefault();
    Map<String, Map<String, ProjectActivitySnapshot>> localData = new LinkedHashMap<>();

    // Convert and collect
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : utcData.entrySet()) {
      String utcHourKey = entry.getKey();
      String localHourKey = convertUtcHourKeyToLocal(utcHourKey, localZone);

      // Merge in case multiple UTC hours map to same local hour (shouldn't happen normally)
      localData.compute(
          localHourKey,
          (key, existingProjects) -> {
            if (existingProjects == null) {
              return new LinkedHashMap<>(entry.getValue());
            }
            existingProjects.putAll(entry.getValue());
            return existingProjects;
          });
    }

    // Sort by key descending (most recent first)
    return localData.entrySet().stream()
        .sorted(Map.Entry.<String, Map<String, ProjectActivitySnapshot>>comparingByKey().reversed())
        .collect(
            LinkedHashMap::new,
            (map, e) -> map.put(e.getKey(), e.getValue()),
            LinkedHashMap::putAll);
  }

  private String convertUtcHourKeyToLocal(String utcHourKey, ZoneId localZone) {
    try {
      LocalDateTime utcDateTime = LocalDateTime.parse(utcHourKey, DATETIME_HOUR_FORMATTER);
      ZonedDateTime localDateTime = utcDateTime.atZone(UTC).withZoneSameInstant(localZone);
      return localDateTime.format(DATETIME_HOUR_FORMATTER);
    } catch (Exception e) {
      // Fallback to original if parsing fails
      return utcHourKey;
    }
  }
}
