package com.codeclocker.plugin.intellij.dashboard;

import com.codeclocker.plugin.intellij.local.LocalActivityDataProvider;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Computes all dashboard metrics from local activity data. */
@Service(Service.Level.APP)
public final class DashboardDataService {

  private static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  public enum TimePeriod {
    LAST_24_HOURS("24h"),
    LAST_7_DAYS("7d"),
    LAST_30_DAYS("30d"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month");

    private final String label;

    TimePeriod(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }

  public record ProjectBreakdownEntry(
      String projectName, long timeSpentSeconds, long additions, long removals) {}

  public record TimelineDataPoint(String label, long seconds) {}

  public record ProjectTimelineEntry(
      String projectName, long totalTimeSeconds, Map<String, Long> dailySeconds) {}

  public record ProjectTimelineData(
      List<String> buckets, List<ProjectTimelineEntry> entries, boolean hourly) {}

  public record DashboardData(
      long totalTimeSpent,
      long dailyAverage,
      long additions,
      long removals,
      int trendPercentage,
      int currentStreak,
      int longestStreak,
      int lifetimeDays,
      long lifetimeTimeSpent,
      int lifetimeProjects,
      long lifetimeLines,
      LocalDate firstActivityDate) {}

  public DashboardData computeForPeriod(TimePeriod period) {
    Map<String, Map<String, ProjectActivitySnapshot>> allData = getAllDataWithUnsaved();

    LocalDate today = LocalDate.now();
    LocalDate periodStart = getPeriodStart(period, today);
    LocalDate periodEnd = getPeriodEnd(period, today);

    // Aggregate for current period
    long totalTime = 0;
    long totalAdditions = 0;
    long totalRemovals = 0;
    Set<String> activeDays = new HashSet<>();

    // For LAST_24_HOURS, we need hour-level filtering
    LocalDateTime cutoff24h =
        period == TimePeriod.LAST_24_HOURS ? LocalDateTime.now().minusHours(24) : null;

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      if (!isInPeriod(hourKey, periodStart, periodEnd, cutoff24h)) {
        continue;
      }
      String dateStr = extractDate(hourKey);
      activeDays.add(dateStr);
      for (ProjectActivitySnapshot snapshot : entry.getValue().values()) {
        totalTime += snapshot.getCodedTimeSeconds();
        totalAdditions += snapshot.getAdditions();
        totalRemovals += snapshot.getRemovals();
      }
    }

    int uniqueActiveDays = activeDays.size();
    long dailyAverage = uniqueActiveDays > 0 ? totalTime / uniqueActiveDays : 0;

    // Trend: compare with previous period of same length
    int trendPercentage = computeTrend(allData, period, today, totalTime);

    // Streaks from all data
    int[] streaks = computeStreaks(allData);
    int currentStreak = streaks[0];
    int longestStreak = streaks[1];

    // Lifetime stats from all data
    Set<String> lifetimeActiveDays = new HashSet<>();
    Set<String> lifetimeProjects = new HashSet<>();
    long lifetimeTime = 0;
    long lifetimeLines = 0;
    LocalDate firstDate = null;

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String dateStr = extractDate(entry.getKey());
      boolean hasActivity = false;
      for (Map.Entry<String, ProjectActivitySnapshot> projEntry : entry.getValue().entrySet()) {
        ProjectActivitySnapshot snapshot = projEntry.getValue();
        if (snapshot.getCodedTimeSeconds() > 0
            || snapshot.getAdditions() > 0
            || snapshot.getRemovals() > 0) {
          hasActivity = true;
          lifetimeProjects.add(projEntry.getKey());
        }
        lifetimeTime += snapshot.getCodedTimeSeconds();
        lifetimeLines += snapshot.getAdditions() + snapshot.getRemovals();
      }
      if (hasActivity) {
        lifetimeActiveDays.add(dateStr);
        try {
          LocalDate date = LocalDate.parse(dateStr);
          if (firstDate == null || date.isBefore(firstDate)) {
            firstDate = date;
          }
        } catch (Exception ignored) {
        }
      }
    }

    return new DashboardData(
        totalTime,
        dailyAverage,
        totalAdditions,
        totalRemovals,
        trendPercentage,
        currentStreak,
        longestStreak,
        lifetimeActiveDays.size(),
        lifetimeTime,
        lifetimeProjects.size(),
        lifetimeLines,
        firstDate);
  }

  public List<TimelineDataPoint> computeTimelineData(TimePeriod period) {
    Map<String, Map<String, ProjectActivitySnapshot>> allData = getAllDataWithUnsaved();
    LocalDate today = LocalDate.now();

    if (period == TimePeriod.LAST_24_HOURS) {
      return computeHourlyTimeline(allData);
    }
    return computeDailyTimeline(
        allData, getPeriodStart(period, today), getPeriodEnd(period, today));
  }

  public List<ProjectBreakdownEntry> computeProjectBreakdown(TimePeriod period) {
    Map<String, Map<String, ProjectActivitySnapshot>> allData = getAllDataWithUnsaved();
    LocalDate today = LocalDate.now();
    LocalDate periodStart = getPeriodStart(period, today);
    LocalDate periodEnd = getPeriodEnd(period, today);

    LocalDateTime cutoff24h =
        period == TimePeriod.LAST_24_HOURS ? LocalDateTime.now().minusHours(24) : null;

    // Accumulate per project: [timeSpent, additions, removals]
    Map<String, long[]> perProject = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      if (!isInPeriod(hourKey, periodStart, periodEnd, cutoff24h)) {
        continue;
      }
      for (Map.Entry<String, ProjectActivitySnapshot> projEntry : entry.getValue().entrySet()) {
        String projectName = projEntry.getKey();
        ProjectActivitySnapshot snapshot = projEntry.getValue();
        long[] acc = perProject.computeIfAbsent(projectName, k -> new long[3]);
        acc[0] += snapshot.getCodedTimeSeconds();
        acc[1] += snapshot.getAdditions();
        acc[2] += snapshot.getRemovals();
      }
    }

    // Filter zero-activity, sort by time descending
    List<ProjectBreakdownEntry> result = new ArrayList<>();
    for (Map.Entry<String, long[]> entry : perProject.entrySet()) {
      long[] acc = entry.getValue();
      if (acc[0] > 0 || acc[1] > 0 || acc[2] > 0) {
        result.add(new ProjectBreakdownEntry(entry.getKey(), acc[0], acc[1], acc[2]));
      }
    }
    result.sort((a, b) -> Long.compare(b.timeSpentSeconds(), a.timeSpentSeconds()));
    return result;
  }

  public ProjectTimelineData computeProjectTimeline(TimePeriod period) {
    Map<String, Map<String, ProjectActivitySnapshot>> allData = getAllDataWithUnsaved();
    LocalDate today = LocalDate.now();
    LocalDate periodStart = getPeriodStart(period, today);
    LocalDate periodEnd = getPeriodEnd(period, today);
    boolean hourly = period == TimePeriod.LAST_24_HOURS;

    LocalDateTime cutoff24h = hourly ? LocalDateTime.now().minusHours(24) : null;

    // Build ordered bucket list
    List<String> buckets = new ArrayList<>();
    if (hourly) {
      LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
      for (int i = 23; i >= 0; i--) {
        buckets.add(now.minusHours(i).format(HOUR_KEY_FORMATTER));
      }
    } else {
      for (LocalDate d = periodStart; !d.isAfter(periodEnd); d = d.plusDays(1)) {
        buckets.add(d.toString());
      }
    }

    // Group by project -> bucket -> seconds
    Map<String, Map<String, Long>> perProject = new LinkedHashMap<>();
    Map<String, Long> projectTotals = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      if (!isInPeriod(hourKey, periodStart, periodEnd, cutoff24h)) {
        continue;
      }
      String bucketKey = hourly ? hourKey : extractDate(hourKey);

      for (Map.Entry<String, ProjectActivitySnapshot> projEntry : entry.getValue().entrySet()) {
        String projectName = projEntry.getKey();
        long seconds = projEntry.getValue().getCodedTimeSeconds();
        if (seconds <= 0) {
          continue;
        }
        perProject
            .computeIfAbsent(projectName, k -> new LinkedHashMap<>())
            .merge(bucketKey, seconds, Long::sum);
        projectTotals.merge(projectName, seconds, Long::sum);
      }
    }

    // Sort by total time descending, take top 10
    List<Map.Entry<String, Long>> sorted = new ArrayList<>(projectTotals.entrySet());
    sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

    List<ProjectTimelineEntry> entries = new ArrayList<>();
    int limit = Math.min(10, sorted.size());
    for (int i = 0; i < limit; i++) {
      String name = sorted.get(i).getKey();
      long total = sorted.get(i).getValue();
      Map<String, Long> daily = perProject.getOrDefault(name, Collections.emptyMap());
      entries.add(new ProjectTimelineEntry(name, total, daily));
    }

    return new ProjectTimelineData(buckets, entries, hourly);
  }

  private List<TimelineDataPoint> computeHourlyTimeline(
      Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
    DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("HH:00");

    // Build ordered map for last 24 hours
    Map<String, Long> hourlyMap = new LinkedHashMap<>();
    for (int i = 23; i >= 0; i--) {
      LocalDateTime hour = now.minusHours(i);
      hourlyMap.put(hour.format(HOUR_KEY_FORMATTER), 0L);
    }

    // Sum data into matching hours
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      if (hourlyMap.containsKey(hourKey)) {
        long sum = 0;
        for (ProjectActivitySnapshot snapshot : entry.getValue().values()) {
          sum += snapshot.getCodedTimeSeconds();
        }
        hourlyMap.merge(hourKey, sum, Long::sum);
      }
    }

    // Convert to data points with display labels
    List<TimelineDataPoint> points = new ArrayList<>();
    for (Map.Entry<String, Long> entry : hourlyMap.entrySet()) {
      try {
        LocalDateTime hour = LocalDateTime.parse(entry.getKey(), HOUR_KEY_FORMATTER);
        points.add(new TimelineDataPoint(hour.format(labelFormatter), entry.getValue()));
      } catch (Exception ignored) {
      }
    }
    return points;
  }

  private List<TimelineDataPoint> computeDailyTimeline(
      Map<String, Map<String, ProjectActivitySnapshot>> allData, LocalDate start, LocalDate end) {
    DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d");

    // Build ordered map for each day in range
    Map<String, Long> dailyMap = new LinkedHashMap<>();
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      dailyMap.put(d.toString(), 0L);
    }

    // Sum data into matching days
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String dateStr = extractDate(entry.getKey());
      if (dailyMap.containsKey(dateStr)) {
        long sum = 0;
        for (ProjectActivitySnapshot snapshot : entry.getValue().values()) {
          sum += snapshot.getCodedTimeSeconds();
        }
        dailyMap.merge(dateStr, sum, Long::sum);
      }
    }

    // Convert to data points with display labels
    List<TimelineDataPoint> points = new ArrayList<>();
    for (Map.Entry<String, Long> entry : dailyMap.entrySet()) {
      try {
        LocalDate date = LocalDate.parse(entry.getKey());
        points.add(new TimelineDataPoint(date.format(labelFormatter), entry.getValue()));
      } catch (Exception ignored) {
      }
    }
    return points;
  }

  private Map<String, Map<String, ProjectActivitySnapshot>> getAllDataWithUnsaved() {
    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    if (dataProvider == null) {
      return Collections.emptyMap();
    }

    Map<String, Map<String, ProjectActivitySnapshot>> allData =
        new LinkedHashMap<>(dataProvider.getAllDataInLocalTimezone());
    mergeUnsavedDeltas(allData);
    return allData;
  }

  private void mergeUnsavedDeltas(Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger == null) {
      return;
    }

    String currentHourKey = LocalDateTime.now().format(HOUR_KEY_FORMATTER);
    Map<String, ProjectActivitySnapshot> hourData =
        allData.computeIfAbsent(currentHourKey, k -> new LinkedHashMap<>());

    Set<String> projectNames = new HashSet<>();
    for (Map<String, ProjectActivitySnapshot> hourEntry : allData.values()) {
      projectNames.addAll(hourEntry.keySet());
    }

    for (String projectName : projectNames) {
      long unsavedDelta = logger.getProjectUnsavedDelta(projectName);
      if (unsavedDelta > 0) {
        ProjectActivitySnapshot existing = hourData.get(projectName);
        if (existing != null) {
          ProjectActivitySnapshot updated =
              new ProjectActivitySnapshot(
                  existing.getCodedTimeSeconds() + unsavedDelta,
                  existing.getAdditions(),
                  existing.getRemovals(),
                  existing.isReported());
          updated.setBranchActivity(existing.getBranchActivity());
          updated.setCommits(existing.getCommits());
          hourData.put(projectName, updated);
        } else {
          hourData.put(projectName, new ProjectActivitySnapshot(unsavedDelta, 0, 0, false));
        }
      }
    }
  }

  private LocalDate getPeriodStart(TimePeriod period, LocalDate today) {
    return switch (period) {
      case LAST_24_HOURS -> today.minusDays(1);
      case LAST_7_DAYS -> today.minusDays(6);
      case LAST_30_DAYS -> today.minusDays(29);
      case THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case THIS_MONTH -> today.withDayOfMonth(1);
    };
  }

  private LocalDate getPeriodEnd(TimePeriod period, LocalDate today) {
    return today;
  }

  private boolean isInPeriod(
      String hourKey, LocalDate start, LocalDate end, LocalDateTime cutoff24h) {
    try {
      if (cutoff24h != null) {
        LocalDateTime hourDateTime = LocalDateTime.parse(hourKey, HOUR_KEY_FORMATTER);
        return !hourDateTime.isBefore(cutoff24h) && !hourDateTime.isAfter(LocalDateTime.now());
      }
      String dateStr = hourKey.substring(0, 10);
      LocalDate date = LocalDate.parse(dateStr);
      return !date.isBefore(start) && !date.isAfter(end);
    } catch (Exception e) {
      return false;
    }
  }

  private int computeTrend(
      Map<String, Map<String, ProjectActivitySnapshot>> allData,
      TimePeriod period,
      LocalDate today,
      long currentTotal) {

    LocalDate currentStart = getPeriodStart(period, today);
    long periodDays = java.time.temporal.ChronoUnit.DAYS.between(currentStart, today) + 1;

    LocalDate prevEnd = currentStart.minusDays(1);
    LocalDate prevStart = prevEnd.minusDays(periodDays - 1);

    // For LAST_24_HOURS, compare previous 24h
    LocalDateTime prevCutoff =
        period == TimePeriod.LAST_24_HOURS ? LocalDateTime.now().minusHours(48) : null;
    LocalDateTime prevEndTime =
        period == TimePeriod.LAST_24_HOURS ? LocalDateTime.now().minusHours(24) : null;

    long prevTotal = 0;
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String hourKey = entry.getKey();
      boolean inPrev;
      if (prevCutoff != null) {
        try {
          LocalDateTime hourDateTime = LocalDateTime.parse(hourKey, HOUR_KEY_FORMATTER);
          inPrev = !hourDateTime.isBefore(prevCutoff) && hourDateTime.isBefore(prevEndTime);
        } catch (Exception e) {
          inPrev = false;
        }
      } else {
        inPrev = isInPeriod(hourKey, prevStart, prevEnd, null);
      }

      if (inPrev) {
        for (ProjectActivitySnapshot snapshot : entry.getValue().values()) {
          prevTotal += snapshot.getCodedTimeSeconds();
        }
      }
    }

    return calculatePercentageChange(currentTotal, prevTotal);
  }

  private static int calculatePercentageChange(long current, long previous) {
    if (previous == 0) {
      return current > 0 ? 100 : 0;
    }
    return (int) Math.round(((double) (current - previous) / previous) * 100);
  }

  private int[] computeStreaks(Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    TreeSet<LocalDate> activeDates = new TreeSet<>();
    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> entry : allData.entrySet()) {
      String dateStr = extractDate(entry.getKey());
      boolean hasTime =
          entry.getValue().values().stream().anyMatch(s -> s.getCodedTimeSeconds() > 0);
      if (hasTime) {
        try {
          activeDates.add(LocalDate.parse(dateStr));
        } catch (Exception ignored) {
        }
      }
    }

    if (activeDates.isEmpty()) {
      return new int[] {0, 0};
    }

    List<LocalDate> sorted = new ArrayList<>(activeDates);

    // Current streak: consecutive days ending today or yesterday
    int currentStreak = 0;
    LocalDate checkDate = LocalDate.now();
    if (!activeDates.contains(checkDate)) {
      checkDate = checkDate.minusDays(1);
    }
    while (activeDates.contains(checkDate)) {
      currentStreak++;
      checkDate = checkDate.minusDays(1);
    }

    // Longest streak
    int longestStreak = 1;
    int runLength = 1;
    for (int i = 1; i < sorted.size(); i++) {
      if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
        runLength++;
        longestStreak = Math.max(longestStreak, runLength);
      } else {
        runLength = 1;
      }
    }

    return new int[] {currentStreak, longestStreak};
  }

  private String extractDate(String hourKey) {
    if (hourKey != null && hourKey.length() >= 10) {
      return hourKey.substring(0, 10);
    }
    return hourKey;
  }
}
