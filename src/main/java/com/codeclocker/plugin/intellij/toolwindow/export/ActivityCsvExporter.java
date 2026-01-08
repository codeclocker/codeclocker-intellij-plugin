package com.codeclocker.plugin.intellij.toolwindow.export;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Exports activity data to CSV format for invoicing purposes. */
public class ActivityCsvExporter {

  private static final String CSV_HEADER = "Date,Project,Hours,Description";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Generates CSV content from activity data.
   *
   * @param data hourKey -> (projectName -> snapshot) map with hourKeys in local timezone
   * @param fromDate start date (inclusive)
   * @param toDate end date (inclusive)
   * @return CSV content as string
   */
  public String exportToCsv(
      Map<String, Map<String, ProjectActivitySnapshot>> data,
      LocalDate fromDate,
      LocalDate toDate) {

    // Aggregate data by date and project
    Map<String, Map<String, DailyProjectData>> dailyData = aggregateByDateAndProject(data);

    // Filter by date range and sort
    List<CsvRow> rows =
        dailyData.entrySet().stream()
            .filter(entry -> isDateInRange(entry.getKey(), fromDate, toDate))
            .sorted(Map.Entry.<String, Map<String, DailyProjectData>>comparingByKey().reversed())
            .flatMap(
                dateEntry ->
                    dateEntry.getValue().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(
                            projectEntry ->
                                new CsvRow(
                                    dateEntry.getKey(),
                                    projectEntry.getKey(),
                                    projectEntry.getValue().totalSeconds,
                                    projectEntry.getValue().commits)))
            .toList();

    return generateCsv(rows);
  }

  /**
   * Gets the date range from the data.
   *
   * @param data hourKey -> (projectName -> snapshot) map
   * @return array with [minDate, maxDate], or null if no data
   */
  public LocalDate[] getDateRange(Map<String, Map<String, ProjectActivitySnapshot>> data) {
    if (data == null || data.isEmpty()) {
      return null;
    }

    LocalDate minDate = null;
    LocalDate maxDate = null;

    for (String hourKey : data.keySet()) {
      LocalDate date = extractDate(hourKey);
      if (date != null) {
        if (minDate == null || date.isBefore(minDate)) {
          minDate = date;
        }
        if (maxDate == null || date.isAfter(maxDate)) {
          maxDate = date;
        }
      }
    }

    if (minDate == null || maxDate == null) {
      return null;
    }

    return new LocalDate[] {minDate, maxDate};
  }

  private Map<String, Map<String, DailyProjectData>> aggregateByDateAndProject(
      Map<String, Map<String, ProjectActivitySnapshot>> data) {

    Map<String, Map<String, DailyProjectData>> result = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : data.entrySet()) {
      String hourKey = hourEntry.getKey();
      String date = extractDateString(hourKey);
      if (date == null) {
        continue;
      }

      for (Map.Entry<String, ProjectActivitySnapshot> projectEntry :
          hourEntry.getValue().entrySet()) {
        String projectName = projectEntry.getKey();
        ProjectActivitySnapshot snapshot = projectEntry.getValue();

        result
            .computeIfAbsent(date, k -> new LinkedHashMap<>())
            .computeIfAbsent(projectName, k -> new DailyProjectData())
            .add(snapshot);
      }
    }

    return result;
  }

  private boolean isDateInRange(String dateStr, LocalDate fromDate, LocalDate toDate) {
    try {
      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      return !date.isBefore(fromDate) && !date.isAfter(toDate);
    } catch (Exception e) {
      return false;
    }
  }

  private String extractDateString(String hourKey) {
    if (hourKey != null && hourKey.length() >= 10) {
      return hourKey.substring(0, 10);
    }
    return null;
  }

  private LocalDate extractDate(String hourKey) {
    String dateStr = extractDateString(hourKey);
    if (dateStr != null) {
      try {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private String generateCsv(List<CsvRow> rows) {
    StringBuilder sb = new StringBuilder();
    sb.append(CSV_HEADER).append("\n");

    for (CsvRow row : rows) {
      sb.append(row.date).append(",");
      sb.append(escapeCsv(row.project)).append(",");
      sb.append(formatHours(row.totalSeconds)).append(",");
      sb.append(escapeCsv(formatCommits(row.commits))).append("\n");
    }

    return sb.toString();
  }

  private String formatHours(long seconds) {
    double hours = seconds / 3600.0;
    return String.format("%.2f", hours);
  }

  private String formatCommits(List<CommitRecord> commits) {
    if (commits == null || commits.isEmpty()) {
      return "";
    }

    return commits.stream()
        .sorted(Comparator.comparingLong(CommitRecord::getTimestamp))
        .map(c -> c.getHash() + ": " + truncateMessage(c.getMessage()))
        .collect(Collectors.joining("; "));
  }

  private String truncateMessage(String message) {
    if (message == null) {
      return "";
    }
    // Take first line only
    int newlineIdx = message.indexOf('\n');
    if (newlineIdx > 0) {
      message = message.substring(0, newlineIdx);
    }
    // Truncate if too long
    if (message.length() > 80) {
      message = message.substring(0, 77) + "...";
    }
    return message;
  }

  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    // If contains comma, quote, or newline, wrap in quotes and escape quotes
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /** Helper class to aggregate daily data per project. */
  private static class DailyProjectData {
    long totalSeconds = 0;
    List<CommitRecord> commits = new ArrayList<>();

    void add(ProjectActivitySnapshot snapshot) {
      totalSeconds += snapshot.getCodedTimeSeconds();
      for (CommitRecord commit : snapshot.getCommits()) {
        if (commits.stream().noneMatch(c -> c.getHash().equals(commit.getHash()))) {
          commits.add(commit);
        }
      }
    }
  }

  /** Helper record for CSV row data. */
  private record CsvRow(
      String date, String project, long totalSeconds, List<CommitRecord> commits) {}
}
